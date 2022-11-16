package internal

import (
	"fmt"
	"strings"
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
			test.runConfig.TestCaseFilePath = fmt.Sprintf("./mock_test_data/testCaseBuilder/%s.json", test.testName)
			err := test.runConfig.UnmarshalInputFile()
			assert.NoError(t, err, "error parsing input file")
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
	tests := []struct {
		testname          string
		runConfig         RunConfig
		expectedTestCases map[string]struct{}
		serviceType       string
		clusters          []Target
	}{
		{
			testname: "eksarm64test",
			runConfig: RunConfig{
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
			clusters: []Target{
				{Name: "eks-cluster-arm64-1", Region: "us-west-2"},
				{Name: "eks-cluster-arm64-2", Region: "us-east-2"},
			},
		},
		{
			testname: "eksoperatortest",
			runConfig: RunConfig{
				IncludedServices: map[string]struct{}{
					"EKS_ADOT_OPERATOR": {},
				},
			},
			expectedTestCases: map[string]struct{}{
				"prometheus_sd_adot_operator": {},
			},
			serviceType: "EKS_ADOT_OPERATOR",
			clusters: []Target{
				{Name: "eks-operator-cluster-1", Region: "us-west-2"},
			},
		},
		{
			testname: "eksoperatorarm64test",
			runConfig: RunConfig{
				IncludedServices: map[string]struct{}{
					"EKS_ADOT_OPERATOR_ARM64": {},
				},
			},
			expectedTestCases: map[string]struct{}{
				"otlp_metric_adot_operator":       {},
				"prometheus_static_adot_operator": {},
			},
			serviceType: "EKS_ADOT_OPERATOR_ARM64",
			clusters: []Target{
				{Name: "eks-operator-arm64-cluster-1", Region: "us-west-2"},
				{Name: "eks-operator-arm64-cluster-2", Region: "us-east-2"},
			},
		},
		{
			testname: "eksfargatetest",
			runConfig: RunConfig{
				IncludedServices: map[string]struct{}{
					"EKS_FARGATE": {},
				},
			},
			expectedTestCases: map[string]struct{}{
				"eks_containerinsights_fargate":        {},
				"eks_containerinsights_fargate_metric": {},
			},
			serviceType: "EKS_FARGATE",
			clusters: []Target{
				{Name: "eks-fargate-cluster-1", Region: "us-west-2"},
				{Name: "eks-fargate-cluster-2", Region: "us-east-2"},
			},
		},
		{
			testname: "ekstest",
			runConfig: RunConfig{
				IncludedServices: map[string]struct{}{
					"EKS": {},
				},
			},
			expectedTestCases: map[string]struct{}{
				"xrayreceiver": {},
				"otlp_metric":  {},
			},
			serviceType: "EKS",
			clusters: []Target{
				{Name: "eks-cluster-1", Region: "us-west-2"},
			},
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			test.runConfig.TestCaseFilePath = fmt.Sprintf("./mock_test_data/testCaseBuilder/%s.json", test.testname)
			err := test.runConfig.UnmarshalInputFile()
			assert.NoError(t, err, "error parsing input file")

			actual, err := buildTestCases(test.runConfig)
			if assert.NoError(t, err) {
				expectedMap := make(map[TestCaseInfo]int)
				for testName := range test.expectedTestCases {
					for _, clustertarget := range test.clusters {
						e := TestCaseInfo{
							testcaseName:  testName,
							serviceType:   test.serviceType,
							additionalVar: strings.Join([]string{clustertarget.Region, clustertarget.Name}, "|"),
						}
						expectedMap[e] = 0
					}

				}

				assert.Equal(t, len(expectedMap), len(actual))
				for _, a := range actual {
					expectedMap[a] = expectedMap[a] + 1
				}

				for testcaseinfo, count := range expectedMap {
					assert.Equal(t, 1, count, fmt.Sprintf("failed for testcase: %v", testcaseinfo))
				}

			}
		})
	}
}
