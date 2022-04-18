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

	var numBatches int
	if len(testCases) <= config.MaxBatches {
		numBatches = len(testCases)
	} else {
		numBatches = config.MaxBatches
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

	// distrubute tests into containers
	for _, tc := range testCases {
		testContainers.Value = append(testContainers.Value.([]TestCaseInfo), tc)
		testContainers.Next()
	}

	// assign containers to a batch
	batchMap := make(map[string]string)
	for i := 0; i < numBatches; i++ {
		batchValueString, err := generateBatchValues(testContainers.Value.([]TestCaseInfo))
		if err != nil {
			return fmt.Errorf("failed to create batchValueString: %w", err)
		}
		batchMap[fmt.Sprintf("batch%d", i)] = batchValueString
		testContainers.Next()
	}

	// create batch key object
	//convert map to array
	var batchArray []string
	for batchhName, _ := range batchMap {
		batchArray = append(batchArray, batchhName)
	}

	batchKeyObject := struct {
		BatchKey []string
	}{
		BatchKey: batchArray,
	}

	githubBatchKeys, err := json.Marshal(batchKeyObject)
	if err != nil {
		return fmt.Errorf("failed to encode batch keys object: %w", err)
	}
	// batch values should imitate `test-case-batch` output
	githubBatchValues, err := json.Marshal(batchMap)
	if err != nil {
		return fmt.Errorf("failed to marshal batch values object: %w", err)
	}

	fmt.Print("::set-output name=batch-keys::" + string(githubBatchKeys))
	fmt.Print("::set-output name=batch-values::" + string(githubBatchValues))

	return nil

}
