package internal

import (
	"context"
	"fmt"
	"strings"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/dynamodb"
	"github.com/aws/aws-sdk-go-v2/service/dynamodb/types"
)

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
		// TODO: Could be improved. DDB API provides batch gets that may be more efficient.
		// This implementation should still operate very efficiently with the scale of our
		// DDB cache.
		ddbOutput, err := svc.GetItem(context.TODO(), &dynamodb.GetItemInput{
			Key: map[string]types.AttributeValue{
				"TestId":      &types.AttributeValueMemberS{Value: testId},
				"aoc_version": &types.AttributeValueMemberS{Value: aocVersion},
			},
			TableName: aws.String(ddbTableName),
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
	fmt.Printf(`release-candidate-ready=%s`, succcess)
	fmt.Printf("\n")

	return nil
}
