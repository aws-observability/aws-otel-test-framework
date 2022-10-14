package internal

import (
	"encoding/json"
	"fmt"
	"os"
	"regexp"
	"strings"
)

var defaultPlatforms = []string{"EKS", "ECS", "EC2", "EKS_ARM64", "EKS_ADOT_OPERATOR", "EKS_ADOT_OPERATOR_ARM64", "EKS_FARGATE"}
var allPlatforms = []string{"EKS", "ECS", "EC2", "EKS_ARM64", "EKS_ADOT_OPERATOR", "EKS_ADOT_OPERATOR_ARM64", "EKS_FARGATE", "CANARY", "PERF", "LOCAL"}

type RunConfig struct {
	OutputLocation   string
	IncludedServices map[string]struct{}
	MaxBatches       int
	TestCaseFilePath string
	TestCaseInput    Tests
}

type Tests struct {
	Tests          []Test          `json:"tests"`
	ClusterTargets []ClusterTarget `json:"clustertargets"`
}

type Test struct {
	CaseName  string   `json:"case_name"`
	Platforms []string `json:"platforms"`
}

type ClusterTarget struct {
	Type    string   `json:"type"`
	Targets []Target `json:"targets"`
}

type Target struct {
	Name   string `json:"name"`
	Region string `json:"region"`
}

func (r *RunConfig) UnmarshalInputFile() error {

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

	ism := stringArrayToMap(defaultPlatforms)
	rc := RunConfig{
		IncludedServices: ism,
		MaxBatches:       40,
	}
	return rc
}

func (r *RunConfig) ValidateTestCaseInput() error {
	// validate clustertargets array
	awsRegionRegex, err := regexp.Compile("^(us(-gov)?|ap|ca|cn|eu|sa)-(central|(north|south)?(east|west)?)-\\d$")
	allPlatformsSet := stringArrayToMap(allPlatforms)
	if err != nil {
		return fmt.Errorf("failed to build regex: %w", err)
	}
	for _, clusterTarget := range r.TestCaseInput.ClusterTargets {
		if _, ok := allPlatformsSet[clusterTarget.Type]; !ok || !strings.HasPrefix(clusterTarget.Type, "EKS") {
			return fmt.Errorf("cluster target type %s is invalid", clusterTarget.Type)
		}

		if len(clusterTarget.Targets) < 1 {
			return fmt.Errorf("must provide at least one cluster for %s platform", clusterTarget.Type)
		}

		for _, cluster := range clusterTarget.Targets {
			if !awsRegionRegex.MatchString(cluster.Region) {
				return fmt.Errorf("invalid aws region: %s", cluster.Region)
			}
			if cluster.Name == "" {
				return fmt.Errorf("missing cluster name in %s cluster target", clusterTarget.Type)
			}
		}
	}

	for _, testCase := range r.TestCaseInput.Tests {
		for _, platform := range testCase.Platforms {
			if _, ok := allPlatformsSet[platform]; !ok {
				return fmt.Errorf("not a valid platform: %s", platform)
			}
		}
	}
	return nil
}

func stringArrayToMap(input []string) map[string]struct{} {
	//build set for default services
	output := make(map[string]struct{})
	for _, val := range input {
		output[val] = struct{}{}
	}
	return output
}
