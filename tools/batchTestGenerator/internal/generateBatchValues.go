package internal

import (
	"fmt"
	"strings"
)

type testSetInput []TestCaseInfo

func generateBatchValues(testSet testSetInput) (string, error) {
	var b strings.Builder

	for _, tsi := range testSet {
		fmt.Fprintf(&b, "%s %s %s\n", tsi.serviceType, tsi.testcaseName, tsi.additionalVar)
	}

	return b.String(), nil
}

func generateBachValuesStringArray(testSet testSetInput) ([]string, error) {
	output := make([]string, 0, len(testSet))
	for _, tsi := range testSet {
		output = append(output, fmt.Sprintf("%s %s %s", tsi.serviceType, tsi.testcaseName, tsi.additionalVar))
	}
	return output, nil
}
