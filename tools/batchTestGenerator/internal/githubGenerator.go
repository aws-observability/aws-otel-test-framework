package internal

import (
	"encoding/json"
	"fmt"
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
	ghEnv, err := os.OpenFile(ghOutputFile, os.O_APPEND|os.O_RDWR|os.O_CREATE, 0600)
	if err != nil {
		fmt.Println("Could not open GITHUB_OUTPUT env file")
		return err
	}

	defer ghEnv.Close()

	//Writing into the gh env fie
	_, err = ghEnv.WriteString(fmt.Sprintf("batch-keys=%s\n", githubBatchKeys))
	if err != nil {
		return fmt.Errorf("error writing githubBatchKeys in GITHUB_OUTPUT env: %v", err)
	}

	_, err = ghEnv.WriteString(fmt.Sprintf("batch-values=%s\n", githubBatchValues))
	if err != nil {
		return fmt.Errorf("error writing githubBatchValues in GITHUB_OUTPUT env: %v", err)
	}

	return nil

}

func createBatchMap(maxBatches int, testCases []TestCaseInfo) (map[string][]string, error) {
	// Group tests by platform so batches never mix platforms
	platformGroups := make(map[string][]TestCaseInfo)
	for _, tc := range testCases {
		platformGroups[tc.serviceType] = append(platformGroups[tc.serviceType], tc)
	}

	// Allocate batch slots proportionally per platform
	batchMap := make(map[string][]string)
	totalTests := len(testCases)

	for platform, tests := range platformGroups {
		// Proportional share of batches for this platform
		share := (len(tests) * maxBatches) / totalTests
		if share < 1 {
			share = 1
		}

		// Calculate tests per batch for this platform
		testsPerBatch := (len(tests) + share - 1) / share

		for i, tc := range tests {
			batchNum := i / testsPerBatch
			id := fmt.Sprintf("%s/%d", platform, batchNum)
			val := fmt.Sprintf("%s %s %s", tc.serviceType, tc.testcaseName, tc.additionalVar)
			batchMap[id] = append(batchMap[id], val)
		}
	}

	return batchMap, nil
}

