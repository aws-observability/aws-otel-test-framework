package internal

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestBuildTestCasesEC2ECS(t *testing.T) {
	tests := []struct {
		testName          string
		runConfig         RunConfig
		expectedTestCases map[string]struct{}
		serviceType       string
		matrixModifier    []string
	}{
		{
			testName: "ec2test",
			runConfig: RunConfig{
				IncludedServices: map[string]struct{}{
					"EC2": {},
				},
			},
			expectedTestCases: map[string]struct{}{
				"xrayreceiver":    {},
				"otlp_metric_amp": {},
			},
			serviceType:    "EC2",
			matrixModifier: ec2AMIs,
		},
		{
			testName: "ecstest",
			runConfig: RunConfig{
				IncludedServices: map[string]struct{}{
					"ECS": {},
				},
			},
			serviceType:    "ECS",
			matrixModifier: ecsLaunchTypes,
			expectedTestCases: map[string]struct{}{
				"xrayreceiver": {},
				"statsd":       {},
			},
		},
	}
	for _, test := range tests {
		t.Run(test.testName, func(t *testing.T) {
			test.runConfig.TestCaseFilePath = fmt.Sprintf("./mock_test_data/%s.json", test.testName)

			actual, err := buildTestCases(test.runConfig)
			if assert.NoError(t, err) {
				expectedTotal := len(test.expectedTestCases) * len(test.matrixModifier)

				// verify counts of test name and ami/launch type
				expectedMap := make(map[TestCaseInfo]int)
				for tn := range test.expectedTestCases {
					for _, mod := range test.matrixModifier {
						c := TestCaseInfo{
							testcaseName:  tn,
							additionalVar: mod,
							serviceType:   test.serviceType,
						}
						expectedMap[c] = 0
					}
				}

				for _, testCaseInfo := range actual {
					expectedMap[testCaseInfo] = expectedMap[testCaseInfo] + 1
				}
				assert.Equal(t, len(expectedMap), expectedTotal)
				for _, count := range expectedMap {
					assert.Equal(t, 1, count)
				}
			}

		})

	}

}

func TestBuildTestCaseEKS(t *testing.T) {
	expectedAdditionalVars := "field1|field2|field3"
	tests := []struct {
		testname          string
		runConfig         RunConfig
		expectedTestCases map[string]struct{}
		serviceType       string
	}{
		{
			testname: "eksarm64test",
			runConfig: RunConfig{
				EksARM64Vars: expectedAdditionalVars,
				IncludedServices: map[string]struct{}{
					"EKS_ARM64": {},
				},
			},
			expectedTestCases: map[string]struct{}{
				"xrayreceiver": {},
				"statsd":       {},
				"statsd_mock":  {},
			},
			serviceType: "EKS_ARM64",
		},
		{
			testname: "eksfargatetest",
			runConfig: RunConfig{
				EksVars: expectedAdditionalVars,
				IncludedServices: map[string]struct{}{
					"EKS_FARGATE": {},
				},
			},
			expectedTestCases: map[string]struct{}{
				"eks_containerinsights_fargate":        {},
				"eks_containerinsights_fargate_metric": {},
			},
			serviceType: "EKS_FARGATE",
		},
		{
			testname: "ekstest",
			runConfig: RunConfig{
				EksVars: expectedAdditionalVars,
				IncludedServices: map[string]struct{}{
					"EKS": {},
				},
			},
			expectedTestCases: map[string]struct{}{
				"xrayreceiver": {},
				"otlp_metric":  {},
			},
			serviceType: "EKS",
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			test.runConfig.TestCaseFilePath = fmt.Sprintf("./mock_test_data/%s.json", test.testname)

			actual, err := buildTestCases(test.runConfig)
			if assert.NoError(t, err) {
				expectedMap := make(map[TestCaseInfo]int)
				for tn := range test.expectedTestCases {
					e := TestCaseInfo{
						testcaseName:  tn,
						serviceType:   test.serviceType,
						additionalVar: expectedAdditionalVars,
					}
					expectedMap[e] = 0
				}

				assert.Equal(t, len(expectedMap), len(actual))
				for _, a := range actual {
					expectedMap[a] = expectedMap[a] + 1
				}

				for tcaseinfo, count := range expectedMap {
					assert.Equal(t, 1, count, fmt.Sprintf("failed for testcase: %v", tcaseinfo))
				}

			}
		})

	}
}
