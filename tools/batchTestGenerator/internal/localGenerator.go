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
	// TODO: randomly chooose test cases
	var numTests int
	if len(testCases) <= config.MaxBatches {
		numTests = len(testCases)
	} else {
		numTests = config.MaxBatches
	}

	finalOutput, err := generateBatchValues(testCases[:numTests])
	if err != nil {
		return fmt.Errorf("failed to generate batch values: %w", err)
	}
	// remove existing file if it exists
	outputFP := filepath.Join(config.OutputLocation, "test-case-batch")
	err = os.Remove(outputFP)
	if err != nil {
		return fmt.Errorf("error when attempting to remove previous file: %w", err)
	}
	err = os.WriteFile(outputFP, []byte(finalOutput), 0644)
	if err != nil {
		return fmt.Errorf("error when writing test-case-batch file: %w", err)
	}

	return nil
}
