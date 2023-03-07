package internal

import (
	"encoding/json"
	"fmt"
	"os"
	"regexp"
	"strings"
)

var defaultPlatformSet = map[string]struct{}{
	"EKS":                     {},
	"ECS":                     {},
	"EC2":                     {},
	"EKS_ARM64":               {},
	"EKS_ADOT_OPERATOR":       {},
	"EKS_ADOT_OPERATOR_ARM64": {},
	"EKS_FARGATE":             {},
}
var allPlatformsSet = map[string]struct{}{"EKS": {},
	"ECS":                     {},
	"EC2":                     {},
	"EKS_ARM64":               {},
	"EKS_ADOT_OPERATOR":       {},
	"EKS_ADOT_OPERATOR_ARM64": {},
	"EKS_FARGATE":             {},
	"CANARY":                  {},
	"PERF":                    {},
	"LOCAL":                   {},
}

var awsRegionRegex = regexp.MustCompile(`^(?:us(?:-gov)?|ap|ca|cn|eu|sa)-(?:central|(?:north|south)?(?:east|west)?)-\d$`)

type RunConfig struct {
	OutputLocation   string
	IncludedServices map[string]struct{}
	MaxBatches       int
	TestCaseFilePath string
	TestCaseInput    TestSuite
}

// TestSuite is used to store unmarshalled testing input file data into.
type TestSuite struct {
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
	Name            string   `json:"name"`
	Region          string   `json:"region"`
	ExcludedTests   []string `json:"excluded_tests"`
	excludedTestSet map[string]struct{}
}

func (t *Target) UnmarshalJSON(b []byte) error {
	type TempTarget Target
	var tempTarget TempTarget
	if err := json.Unmarshal(b, &tempTarget); err != nil {
		return err
	}
	*t = Target(tempTarget)
	t.excludedTestSet = make(map[string]struct{}, 0)
	for _, val := range t.ExcludedTests {
		t.excludedTestSet[val] = struct{}{}
	}
	return nil
}

func (t *Target) isTestCaseExcluded(value string) bool {
	_, contains := t.excludedTestSet[value]
	return contains
}

func (r *RunConfig) unmarshalInputFile() error {

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
	rc := RunConfig{
		IncludedServices: defaultPlatformSet,
		MaxBatches:       40,
	}
	return rc
}

func (r *RunConfig) validateTestCaseInput() error {
	for _, clusterTarget := range r.TestCaseInput.ClusterTargets {
		if !isValidClusterType(clusterTarget.Type) {
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

func (r *RunConfig) InitInputFile() error {
	err := r.unmarshalInputFile()
	if err != nil {
		return fmt.Errorf("error parsing input file: %w", err)
	}

	err = r.validateTestCaseInput()
	if err != nil {
		return fmt.Errorf("input file validation failed: %w", err)
	}
	return nil
}

func isValidClusterType(targetTypeInput string) bool {
	if _, ok := allPlatformsSet[targetTypeInput]; !ok {
		return false
	}

	if !strings.HasPrefix(targetTypeInput, "EKS") {
		return false
	}
	return true
}
