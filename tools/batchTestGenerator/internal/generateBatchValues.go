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
