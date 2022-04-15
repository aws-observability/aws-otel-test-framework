package internal

import (
	"fmt"
	"os"
	"path/filepath"
)

// generates a `test-case-batch` file for local use and store it in output
// location in runconfig
func LocalGenerator(config RunConfig) error {
	testCases, err := buildTestCases(config)
	if err != nil {
		return fmt.Errorf("failed to build test case: %w", err)
	}
	// take the first N jobs where N is the MAXBATCHES values
	// TODO: randomly generate tests

	var numTests int
	if len(testCases) <= config.MaxBatches {
		numTests = len(testCases) - 1
	} else {
		numTests = config.MaxBatches
	}

	finalOutput, err := generateBatchValues(testCases[:numTests])
	if err != nil {
		return fmt.Errorf("failed to generate batch values: %w", err)
	}

	os.WriteFile(filepath.Join(config.OutputLocation, "test-case-batch"), []byte(finalOutput), 0644)

	return nil
}
