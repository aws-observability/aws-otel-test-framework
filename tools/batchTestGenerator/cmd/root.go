package cmd

import (
	"fmt"
	batch "github.com/aws-observability/aws-otel-test-framework/batchTestGenerator/internal"
	"github.com/spf13/cobra"
	"log"
	"os"
	"path/filepath"
)

type eksFields struct {
	region      string
	ampEndpoint string
	clusterName string
}

type commandConfig struct {
	runConfig       batch.RunConfig
	rootCommand     cobra.Command
	githubCommand   cobra.Command
	localCommand    cobra.Command
	validateCommand cobra.Command
	includeFlags    []string
	eksARM64Flags   eksFields
	eksFlags        eksFields
	// used in the validate command
	// DyanmoDB Table that was used a successful test cache
	DynamoDBTable string
	// Name of ADOT Collector image that was used. Will be used
	// as sort key when querying cache.
	AocVersion string
}

var includeAllowlist map[string]struct{} = map[string]struct{}{
	"EKS":                     {},
	"EKS_ARM64":               {},
	"ECS":                     {},
	"EKS_FARGATE":             {},
	"EC2":                     {},
	"EKS_ADOT_OPERATOR":       {},
	"EKS_ADOT_OPERATOR_ARM64": {},
}

func newCommandConfig() *commandConfig {
	c := &commandConfig{
		runConfig: batch.NewDefaultRunConfig(),
	}

	preRunSetup := func(cmd *cobra.Command, args []string) error {
		var err error
		if len(c.includeFlags) > 0 {

			c.runConfig.IncludedServices, err = transformInclude(c.includeFlags)
			if err != nil {
				return fmt.Errorf("failed to map included services: %w", err)
			}
		}

		if c.runConfig.TestCaseFilePath == "" {
			//validate input/output set to cwd if necessary
			defaultFileLoc, err := os.Getwd()
			if err != nil {
				return fmt.Errorf("failed to get cwd: %w", err)
			}
			c.runConfig.TestCaseFilePath = filepath.Join(defaultFileLoc, "testcases.json")
		}

		if c.runConfig.MaxBatches < 1 {
			return fmt.Errorf("max batches must be greater than 0")
		}

		err = c.runConfig.ParseInputFile()
		if err != nil {
			return fmt.Errorf("error parsing input file: %w", err)
		}

		return nil
	}

	c.rootCommand = cobra.Command{
		Use:               "batchTestGenerator",
		Short:             "Generate test batches for GitHub Actions or local use",
		Long:              ``,
		PersistentPreRunE: preRunSetup,
	}

	c.localCommand = cobra.Command{
		Use:   "local",
		Short: "Generate a local test-case-batch file",
		Long: `Creates a local test-case-batch file that can be utilized
		to run a set of tests synchronously. See README for more information`,
		PreRunE: func(cmd *cobra.Command, args []string) error {
			if c.runConfig.OutputLocation == "" {
				//validate input/output set to cwd if necessary
				defaultFileLoc, err := os.Getwd()
				if err != nil {
					return fmt.Errorf("failed to get cwd: %w", err)
				}
				c.runConfig.OutputLocation = defaultFileLoc
			}
			return nil
		},
		RunE: func(cmd *cobra.Command, args []string) error {
			return batch.LocalGenerator(c.runConfig)
		},
	}

	c.githubCommand = cobra.Command{
		Use:   "github",
		Short: "Genereate two outputs for GitHub Actions",
		Long: `This command generates a batch-test-key and batch-test-values
		output for use in GitHub Actions. See README for example usage.`,
		RunE: func(cmd *cobra.Command, args []string) error {
			return batch.GithubGenerator(c.runConfig)
		},
	}

	c.validateCommand = cobra.Command{
		Use:   "validate",
		Short: "Validate all test cases are present in the cache",
		Long: `This command verifies that all tests succesffully passed
		and are thus present in the DynamoDB table name that was provided.
		EKS and EKS_ARM64 additional vars that are identical to the ones passed
		into the github command must be provided also.`,
		RunE: func(cmd *cobra.Command, args []string) error {
			return batch.ValidateCache(c.runConfig, c.DynamoDBTable, c.AocVersion)
		},
	}
	c.rootCommand.AddCommand(&c.localCommand)
	c.rootCommand.AddCommand(&c.githubCommand)
	c.rootCommand.AddCommand(&c.validateCommand)

	return c
}

var (
	comCfg = newCommandConfig()
)

func Execute() {
	err := comCfg.rootCommand.Execute()
	if err != nil {
		log.Printf("failed execute: %v", err)
		os.Exit(1)
	}
}

func init() {
	// Persistent Flags
	comCfg.rootCommand.PersistentFlags().StringVar(&comCfg.runConfig.TestCaseFilePath, "testCaseFilePath", "", `path to JSON test case file`)
	comCfg.rootCommand.PersistentFlags().StringSliceVar(&comCfg.includeFlags, "include", []string{}, "list of commma separated services to include. See README for list of valid values.")

	// githubflags only
	comCfg.githubCommand.Flags().IntVar(&comCfg.runConfig.MaxBatches, "maxBatch", 40, "Maxium number of batches allowed.")

	// local flags only
	comCfg.localCommand.Flags().StringVar(&comCfg.runConfig.OutputLocation, "output", "", "Output location for test-case-batch file.")
	comCfg.localCommand.Flags().IntVar(&comCfg.runConfig.MaxBatches, "maxJobs", 5, "Maximum number of jobs allowed in test-case-batch file. Will generate tests up to this amount if possible from"+
		" provided test cases and included services. ")

	// validate flags only
	comCfg.validateCommand.Flags().StringVar(&comCfg.DynamoDBTable, "ddbtable", "", "name of the DynamoDB table that was used a successful test cache")
	comCfg.validateCommand.Flags().StringVar(&comCfg.AocVersion, "aocVersion", "", "name of the ADOT Collector Image used in testing")
	comCfg.validateCommand.MarkFlagRequired("ddbtable")
	comCfg.validateCommand.MarkFlagRequired("aocVersion")
}

// transform array slice into map
func transformInclude(includedServices []string) (map[string]struct{}, error) {

	output := make(map[string]struct{})
	for _, val := range includedServices {
		if _, ok := includeAllowlist[val]; ok {
			output[val] = struct{}{}
		} else {
			return nil, fmt.Errorf("invalid service in --include flag")
		}
	}
	return output, nil
}
