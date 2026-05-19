package internal

import (
	"encoding/json"
	"fmt"
	"os"
	"strings"
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

func displayVariant(serviceType, additionalVar string) string {
	if strings.Contains(additionalVar, "|") {
		parts := strings.SplitN(additionalVar, "|", 2)
		return strings.TrimPrefix(parts[1], "collector-ci-")
	}
	return additionalVar
}

func createBatchMap(maxBatches int, testCases []TestCaseInfo) (map[string][]string, error) {
	// Group tests by platform + variant (AMI for EC2, cluster for EKS, launch type for ECS)
	subGroups := make(map[string][]TestCaseInfo)
	for _, tc := range testCases {
		key := fmt.Sprintf("%s/%s", tc.serviceType, displayVariant(tc.serviceType, tc.additionalVar))
		subGroups[key] = append(subGroups[key], tc)
	}

	// Allocate batch slots proportionally per sub-group
	batchMap := make(map[string][]string)
	totalTests := len(testCases)

	for groupKey, tests := range subGroups {
		// Separate kafka tests into their own batch (they share MSK connections)
		var kafkaTests []TestCaseInfo
		var otherTests []TestCaseInfo
		for _, tc := range tests {
			if strings.Contains(tc.testcaseName, "kafka") {
				kafkaTests = append(kafkaTests, tc)
			} else {
				otherTests = append(otherTests, tc)
			}
		}
		if len(kafkaTests) > 0 {
			// Split kafka into batches of 2 (each test takes ~10min, 2 fits in 30m timeout)
			for i, tc := range kafkaTests {
				batchNum := i / 2
				id := fmt.Sprintf("%s/kafka-%d", groupKey, batchNum)
				val := fmt.Sprintf("%s %s %s", tc.serviceType, tc.testcaseName, tc.additionalVar)
				batchMap[id] = append(batchMap[id], val)
			}
		}
		tests = otherTests

		share := (len(tests) * maxBatches) / totalTests
		if share < 1 {
			share = 1
		}

		testsPerBatch := (len(tests) + share - 1) / share
		if strings.HasPrefix(groupKey, "ECS") && testsPerBatch > 2 {
			testsPerBatch = 2
		}
		if strings.Contains(groupKey, "windows") && testsPerBatch > 3 {
			testsPerBatch = 3
		}

		for i, tc := range tests {
			batchNum := i / testsPerBatch
			var id string
			if testsPerBatch == 1 || len(tests) <= share {
				id = fmt.Sprintf("%s/%s", groupKey, tc.testcaseName)
			} else {
				id = fmt.Sprintf("%s/batch-%d", groupKey, batchNum)
			}
			val := fmt.Sprintf("%s %s %s", tc.serviceType, tc.testcaseName, tc.additionalVar)
			batchMap[id] = append(batchMap[id], val)
		}
	}

	return batchMap, nil
}

