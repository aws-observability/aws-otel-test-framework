package internal

import (
	"encoding/json"
	"fmt"
	"os"
)

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

var ec2AMIs = []string{
	"amazonlinux2",
	"amazonlinux",
	"ubuntu18",
	"ubuntu16",
	"debian10",
	"debian9",
	"suse15",
	"suse12",
	"redhat8",
	"redhat7",
	"centos7",
	"windows2019",
	"arm_amazonlinux2",
	"arm_suse15",
	"arm_redhat8",
	"arm_redhat7",
	"arm_ubuntu18",
	"arm_ubuntu16",
}

var ecsLaunchTypes = []string{"EC2", "FARGATE"}

func buildTestCases(runConfig RunConfig) ([]TestCaseInfo, error) {
	testCases := []TestCaseInfo{}
	var tests Tests

	testCaseFile, err := os.ReadFile(runConfig.TestCaseFilePath)
	if err != nil {
		return nil, fmt.Errorf("failed to read test cases file: %w", err)
	}

	err = json.Unmarshal(testCaseFile, &tests)
	if err != nil {
		return nil, fmt.Errorf("failed to unmarshal test case file: %w", err)
	}

	// iterate through all test cases to build output info
	for _, test := range tests.Tests {
		// iterate through platforms in test case
		for _, testPlatform := range test.Platforms {
			// see if we are including this platform in run config
			if _, ok := runConfig.IncludedServices[testPlatform]; ok {
				// platform included add it to final output
				var newTests []TestCaseInfo
				switch testPlatform {
				case "ECS":
					for _, launchType := range ecsLaunchTypes {
						newTest := TestCaseInfo{
							testcaseName:  test.CaseName,
							serviceType:   testPlatform,
							additionalVar: launchType,
						}
						newTests = append(newTests, newTest)
					}
				case "EC2":
					for _, ami := range ec2AMIs {
						newTest := TestCaseInfo{
							testcaseName:  test.CaseName,
							serviceType:   testPlatform,
							additionalVar: ami,
						}
						newTests = append(newTests, newTest)
					}
				case "EKS_ARM64", "EKS_ADOT_OPERATOR_ARM64":
					newTest := TestCaseInfo{
						testcaseName:  test.CaseName,
						serviceType:   testPlatform,
						additionalVar: runConfig.EksARM64Vars,
					}
					newTests = append(newTests, newTest)
				case "EKS_FARGATE", "EKS_ADOT_OPERATOR", "EKS":
					newTest := TestCaseInfo{
						testcaseName:  test.CaseName,
						serviceType:   testPlatform,
						additionalVar: runConfig.EksVars,
					}
					newTests = append(newTests, newTest)
				default:
					return nil, fmt.Errorf("platform not recognized: %s", testPlatform)
				}
				testCases = append(testCases, newTests...)
			}
		}
	}

	return testCases, nil
}
