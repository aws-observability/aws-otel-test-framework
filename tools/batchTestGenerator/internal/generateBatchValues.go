package internal

import (
	"fmt"
	"strings"
)

type testSetInput []TestCaseInfo

func generateBatchValues(testSet testSetInput) (string, error) {
	var formattedTestCase []string

	for _, tsi := range testSet {
		currentTestCase := fmt.Sprintf("%s %s %s", tsi.serviceType, tsi.testcaseName, tsi.additionalVar)
		formattedTestCase = append(formattedTestCase, currentTestCase)
	}
	return strings.Join(formattedTestCase, "\n"), nil
}
