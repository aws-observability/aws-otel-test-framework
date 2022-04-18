package internal

import (
	"container/ring"
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
		testContainers.Value = make([]TestCaseInfo, 1)
		testContainers = testContainers.Next()
	}

	// distrubute tests into containers
	for _, tc := range testCases {
		testContainers.Value = append(testContainers.Value.([]TestCaseInfo), tc)
		testContainers.Next()
	}

	// assign containers to a batch
	batchMap := make(map[string][]TestCaseInfo)
	for i := 0; i < numBatches; i++ {
		batchMap[fmt.Sprintf("batch%d", i)] = testContainers.Value.([]TestCaseInfo)
		testContainers.Next()
	}

	return nil

}
