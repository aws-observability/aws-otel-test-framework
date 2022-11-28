package internal

import (
	"container/ring"
	"encoding/json"
	"fmt"
	"log"
	"os"
)

// generates the batch keys and value json for github action utilization

func GithubGenerator(config RunConfig) error {
	testCases, err := buildTestCases(config)
	if err != nil {
		return fmt.Errorf("failed to build test case: %w", err)
	}

	batchMap, err := createBatchMap(config.MaxBatches, testCases)
	if err != nil {
		return fmt.Errorf("failed to create batch map: %w", err)
	}

	// create batch key object
	// convert map to array
	var batchArray []string
	for batchName := range batchMap {
		batchArray = append(batchArray, batchName)
	}

	batchKeyJSONObject := struct {
		BatchKey []string
	}{
		BatchKey: batchArray,
	}

	githubBatchKeys, err := json.Marshal(batchKeyJSONObject)
	if err != nil {
		return fmt.Errorf("failed to encode batch keys object: %w", err)
	}
	// batch values should imitate `test-case-batch` output
	githubBatchValues, err := json.Marshal(batchMap)
	if err != nil {
		return fmt.Errorf("failed to marshal batch values object: %w", err)
	}
	ghOutputFile := os.Getenv("GITHUB_OUTPUT")
	ghOutputFile, err = os.OpenFile(ghOutputFile, os.O_APPEND|os.O_RDWR|os.O_CREATE, 0600)
	if err != nil {
		log.Fatal(err)
	}
	defer ghOutputFile.Close()

	_, err = ghOutputFile.WriteString(fmt.Sprintf(`batch-keys=%s`, githubBatchKeys))
	if err != nil {
		fmt.Printf("error writing githubBatchKeys in GITHUB_OUTPUT: %v", err)
	}
	fmt.Printf("\n")

	_, err = ghOutputFile.WriteString(fmt.Sprintf(`batch-values=%s`, githubBatchValues))
	if err != nil {
		fmt.Printf("error writing githubBatchValues in GITHUB_OUTPUT: %v", err)
	}
	fmt.Printf("\n")
	ghOutputFile, err = os.LookupEnv("GITHUB_OUTPUT")
	if err != nil {
		log.Fatal(err)
	}

	return nil

}

func createBatchMap(maxBatches int, testCases []TestCaseInfo) (map[string][]string, error) {
	var numBatches int
	if len(testCases) <= maxBatches {
		numBatches = len(testCases)
	} else {
		numBatches = maxBatches
	}

	nonParallelTestSet := map[string][]TestCaseInfo{
		"EKS_ADOT_OPERATOR":       {},
		"EKS_ADOT_OPERATOR_ARM64": {},
		"EKS_FARGATE":             {},
		"EKS_ARM64":               {},
	}

	if numBatches == 1 {
		nonParallelTestSet = map[string][]TestCaseInfo{}
	} else if numBatches-len(nonParallelTestSet) <= 0 {
		numBatches = 1
	} else {
		numBatches -= len(nonParallelTestSet)
	}

	// circular linked list to distribute values
	// we reach for a circular LL to evenly distrubute values since no
	// weighting is being done during the batching process. We just want the
	// easiest way to distribute test cases.
	testContainers := ring.New(numBatches)
	for i := 0; i < numBatches; i++ {
		testContainers.Value = make([]TestCaseInfo, 0)
		testContainers = testContainers.Next()
	}

	// distribute tests into containers
	for _, tc := range testCases {
		if _, ok := nonParallelTestSet[tc.serviceType]; ok {
			nonParallelTestSet[tc.serviceType] = append(nonParallelTestSet[tc.serviceType], tc)
		} else {
			testContainers.Value = append(testContainers.Value.([]TestCaseInfo), tc)
			testContainers = testContainers.Next()
		}

	}

	// assign containers to a batch
	batchMap := make(map[string][]string)

	batch := 0
	// non-parallel tests
	for _, npts := range nonParallelTestSet {
		if len(npts) == 0 {
			continue
		}

		nptsStringArray, err := generateBachValuesStringArray(npts)
		if err != nil {
			return nil, fmt.Errorf("failed to create non parallel test set string array: %w", err)
		}
		id := fmt.Sprintf("batch%d", batch)
		batchMap[id] = append(batchMap[id], nptsStringArray...)
		if batch < maxBatches {
			batch++
		}
	}

	//assign following batches
	for i := 0; i < numBatches; i++ {
		ts := testContainers.Value.([]TestCaseInfo)
		if len(ts) == 0 {
			testContainers = testContainers.Next()
			continue
		}

		batchValueStringArray, err := generateBachValuesStringArray(ts)
		if err != nil {
			return nil, fmt.Errorf("failed to create batchValueString: %w", err)
		}
		id := fmt.Sprintf("batch%d", batch)
		batchMap[id] = append(batchMap[id], batchValueStringArray...)
		testContainers = testContainers.Next()
		if batch < maxBatches {
			batch++
		}
	}

	return batchMap, nil
}
