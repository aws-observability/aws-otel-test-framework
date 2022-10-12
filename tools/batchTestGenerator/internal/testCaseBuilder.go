package internal

import (
	"fmt"
	"strings"
)

var ec2AMIs = []string{
	"ubuntu18",
	"arm_ubuntu18",
	"ubuntu20",
	"arm_ubuntu20",
	"ubuntu22",
	"arm_ubuntu22",
	"debian11",
	"arm_debian11",
	"debian10",
	"arm_debian10",
	"amazonlinux2",
	"arm_amazonlinux2",
	"windows2022",
	"windows2019",
	"suse15",
	"suse12",
	"redhat8",
	"arm_redhat8",
}

type TestCaseInfo struct {
	testcaseName  string
	serviceType   string
	additionalVar string
}

var ecsLaunchTypes = []string{"EC2", "FARGATE"}

func buildTestCases(runConfig RunConfig) ([]TestCaseInfo, error) {
	testCases := []TestCaseInfo{}
	tests := runConfig.TestCaseInput
	clusterMaps := clusterMap(tests.ClusterTargets)
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
				case "EKS_FARGATE", "EKS_ADOT_OPERATOR", "EKS_ADOT_OPERATOR_ARM64", "EKS", "EKS_ARM64":
					if clusterList, ok := clusterMaps[testPlatform]; ok {
						for _, cluster := range clusterList {
							newTest := TestCaseInfo{
								testcaseName:  test.CaseName,
								serviceType:   testPlatform,
								additionalVar: strings.Join([]string{cluster.Region, cluster.Name}, "|"),
							}
							newTests = append(newTests, newTest)
						}
					} else {
						return nil, fmt.Errorf("no clusters defined for %s plaftorm in test input file", testPlatform)
					}

				default:
					return nil, fmt.Errorf("platform not recognized: %s", testPlatform)
				}
				testCases = append(testCases, newTests...)
			}
		}
	}
	return testCases, nil
}

func clusterMap(inputTargets []ClusterTarget) map[string][]Target {
	outputMap := make(map[string][]Target)
	for _, clusterTarget := range inputTargets {
		outputMap[clusterTarget.Type] = clusterTarget.Targets
	}
	return outputMap
}
