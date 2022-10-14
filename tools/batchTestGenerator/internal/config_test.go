package internal

import (
	"fmt"
	"github.com/stretchr/testify/assert"
	"path/filepath"
	"testing"
)

func TestRunConfig_ValidateTestCaseInput(t *testing.T) {
	tests := []struct {
		testname     string
		expectedFunc assert.ErrorAssertionFunc
	}{
		{
			testname:     "config_input_bad_platform",
			expectedFunc: assert.Error,
		},
		{
			testname:     "config_input_bad_region",
			expectedFunc: assert.Error,
		},
		{
			testname:     "config_input_no_cluster",
			expectedFunc: assert.Error,
		},
		{
			testname:     "config_input_no_region",
			expectedFunc: assert.Error,
		},
		{
			testname:     "config_input_no_target",
			expectedFunc: assert.Error,
		},
		{
			testname:     "config_input_valid",
			expectedFunc: assert.NoError,
		},
	}

	for _, test := range tests {
		rc := NewDefaultRunConfig()
		rc.TestCaseFilePath = filepath.Join(".", "mock_test_data", "config", fmt.Sprintf("%s.json", test.testname))
		err := rc.UnmarshalInputFile()
		assert.NoError(t, err)
		actual := rc.ValidateTestCaseInput()
		test.expectedFunc(t, actual)
	}
}
