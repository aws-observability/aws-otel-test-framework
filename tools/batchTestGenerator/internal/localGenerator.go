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

	finalOutput, err := generateBatchValues(testCases)
	if err != nil {
		return fmt.Errorf("failed to generate batch values: %w", err)
	}

	os.WriteFile(filepath.Join(config.OutputLocation, "test-case-batch"), []byte(finalOutput), 0644)

	return nil
}
