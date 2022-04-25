package internal

import (
	"container/ring"
	"encoding/json"
	"fmt"
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

	fmt.Printf(`::set-output name=batch-keys::%s`, githubBatchKeys)
	fmt.Printf("\n")
	fmt.Printf(`::set-output name=batch-values::%s`, githubBatchValues)
	fmt.Printf("\n")

	return nil

}

func createBatchMap(maxBatches int, testCases []TestCaseInfo) (map[string][]string, error) {
	var numBatches int
	if len(testCases) <= maxBatches {
		numBatches = len(testCases)
	} else {
		numBatches = maxBatches
	}

	// This will be used to store all tests that cannot be run in parallel.
	// Will also be used if only one batch is allowed
	nonParallelTests := make([]TestCaseInfo, 0)
	nonParallelTestSet := map[string]struct{}{
		"EKS_ADOT_OPERATOR": {},
		"EKS_FARGATE":       {},
	}
	// circular linked list to distribute values
	// we reach for a circular LL to evenly distrubute values since no
	// weighting is being done during the batching process. We just want the
	// easiest way to distribute test cases.
	// numBatches - 1 since a batch is already defined above.
	testContainers := ring.New(numBatches - 1)
	for i := 0; i < numBatches; i++ {
		testContainers.Value = make([]TestCaseInfo, 0)
		testContainers = testContainers.Next()
	}

	// distrubute tests into containers
	for _, tc := range testCases {
		if _, ok := nonParallelTestSet[tc.serviceType]; ok || numBatches == 1 {
			nonParallelTests = append(nonParallelTests, tc)
		} else {
			testContainers.Value = append(testContainers.Value.([]TestCaseInfo), tc)
			testContainers = testContainers.Next()
		}

	}

	// assign containers to a batch
	batchMap := make(map[string][]string)

	// batch 0 will always be non parallel tests or all tests
	nptsStringArray, err := generateBachValuesStringArray(nonParallelTests)
	if err != nil {
		return nil, fmt.Errorf("failed to create non parallel test set string array: %w", err)
	}
	batchMap[fmt.Sprintf("batch%d", 0)] = nptsStringArray

	//assign following batches
	for i := 1; i < numBatches; i++ {
		batchValueStringArray, err := generateBachValuesStringArray(testContainers.Value.([]TestCaseInfo))
		if err != nil {
			return nil, fmt.Errorf("failed to create batchValueString: %w", err)
		}
		batchMap[fmt.Sprintf("batch%d", i)] = batchValueStringArray
		testContainers = testContainers.Next()
	}

	return batchMap, nil
}
