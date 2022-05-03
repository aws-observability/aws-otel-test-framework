package internal

import (
	"context"
	"fmt"
	"strings"

	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/feature/dynamodb/attributevalue"
	"github.com/aws/aws-sdk-go-v2/service/dynamodb"
)

type TableKeys struct {
	TestId      string
	aoc_version string
}

func ValidateCache(rc RunConfig, ddbTableName string, aocVersion string) error {
	cfg, err := config.LoadDefaultConfig(context.TODO(), config.WithRegion("us-west-2"))
	if err != nil {
		return fmt.Errorf("unable to load SDK config, %w", err)
	}

	// Using the Config value, create the DynamoDB client
	svc := dynamodb.NewFromConfig(cfg)

	testCases, err := buildTestCases(rc)
	if err != nil {
		return fmt.Errorf("failed to build test case: %w", err)
	}

	cacheMisses := make([]string, 0)

	for _, tc := range testCases {
		testId := fmt.Sprintf("%s%s%s", tc.serviceType, tc.testcaseName, tc.additionalVar)
		tableKeys := TableKeys{
			TestId:      testId,
			aoc_version: aocVersion,
		}
		tk, err := attributevalue.MarshalMap(tableKeys)
		if err != nil {
			return fmt.Errorf("failed to marshal table keys: %w", err)
		}

		ddbOutput, err := svc.GetItem(context.TODO(), &dynamodb.GetItemInput{
			Key:       tk,
			TableName: &ddbTableName,
		})
		if err != nil {
			return fmt.Errorf("failed to query ddb table: %w", err)
		}

		if ddbOutput.Item == nil {
			cacheMisses = append(cacheMisses, fmt.Sprintf("%s %s %s", tc.serviceType, tc.testcaseName, tc.additionalVar))
		}
	}

	succcess := "true"
	if len(cacheMisses) != 0 {
		fmt.Println("Cache Misses:")
		fmt.Println(strings.Join(cacheMisses, "\n"))
		succcess = "false"
	}
	fmt.Printf(`::set-output name=release-candidate-ready::%s`, succcess)
	fmt.Printf("\n")

	return nil
}
