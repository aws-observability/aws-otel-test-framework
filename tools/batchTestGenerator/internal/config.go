package internal

import (
	"encoding/json"
	"fmt"
	"os"
)

type RunConfig struct {
	OutputLocation   string
	IncludedServices map[string]struct{}
	MaxBatches       int
	TestCaseFilePath string
	TestCaseInput    Tests
}

type Tests struct {
	Tests []Test `json:"tests"`
}

type Test struct {
	CaseName  string   `json:"case_name"`
	Platforms []string `json:"platforms"`
}

type TestCaseInfo struct {
	testcaseName  string
	serviceType   string
	additionalVar string
}

func (r *RunConfig) ParseInputFile() error {

	testCaseFile, err := os.ReadFile(r.TestCaseFilePath)
	if err != nil {
		return fmt.Errorf("failed to read test cases file: %w", err)
	}

	err = json.Unmarshal(testCaseFile, &r.TestCaseInput)
	if err != nil {
		return fmt.Errorf("failed to unmarshal test case file: %w", err)
	}
	return nil
}

func NewDefaultRunConfig() RunConfig {
	defaultServices := []string{"EKS", "ECS", "EC2", "EKS_ARM64", "EKS_ADOT_OPERATOR", "EKS_ADOT_OPERATOR_ARM64", "EKS_FARGATE"}

	//build set for default services
	ism := make(map[string]struct{})
	for _, ds := range defaultServices {
		ism[ds] = struct{}{}
	}

	rc := RunConfig{
		IncludedServices: ism,
		MaxBatches:       40,
	}
	return rc
}
