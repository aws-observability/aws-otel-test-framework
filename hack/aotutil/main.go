// Copyright 2021 Amazon.com, Inc. or its affiliates
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"context"
	"log"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

func main() {
	logger, err := zap.NewDevelopment()
	if err != nil {
		log.Fatal(err)
	}

	ctx := context.Background()
	var aFlags AwsFlags
	cmdCtx := CmdContext{
		Logger:   logger,
		AwsFlags: &aFlags,
	}
	root := cobra.Command{
		Use:   "aotutil",
		Short: "Utils for aws observability test framework",
	}
	root.PersistentFlags().StringVar(&aFlags.Region, "region", "us-west-2", "aws region name")
	root.AddCommand(
		ssmCmd(ctx, &cmdCtx),
	)
	if err := root.Execute(); err != nil {
		log.Fatal(err)
	}
}

type CmdContext struct {
	Logger   *zap.Logger
	AwsFlags *AwsFlags
}

type AwsFlags struct {
	Region string
}

func (f *AwsFlags) MustLoadConfig(ctx context.Context, logger *zap.Logger) aws.Config {
	cfg, err := config.LoadDefaultConfig(ctx, config.WithRegion("us-west-2"))
	if err != nil {
		logger.Fatal("Init AWS config failed", zap.Error(err))
	}
	return cfg
}

func ssmCmd(ctx context.Context, cmdCtx *CmdContext) *cobra.Command {
	logger := cmdCtx.Logger.With(zap.String("Command", "ssm"))
	var (
		ssmWrapper     *SSMWrapper
		ssmWaitTimeout time.Duration
	)
	root := cobra.Command{
		Use:   "ssm",
		Short: "Wrapper for SSm (AWS Systems Manager)",
		PersistentPreRun: func(cmd *cobra.Command, args []string) {
			cfg := cmdCtx.AwsFlags.MustLoadConfig(ctx, logger)
			ssmWrapper = NewSSM(cfg, logger)
		},
	}
	root.Flags().DurationVar(&ssmWaitTimeout, "timeout", 15*time.Minute, "abort polling if timeout duration is exceeded")

	oneInstanceId := func(args []string) string {
		if len(args) == 0 {
			logger.Fatal("Instance ID is required as position arguments")
		}
		if len(args) > 1 {
			logger.Warn("Only one instance ID is supported", zap.Int("Got", len(args)))
		}
		return args[0]
	}
	// Wait Patch
	waitPatch := cobra.Command{
		Use:   "wait-patch",
		Short: "Wait until patch association is completed or timeout",
		Example: `# Wait patch on ec2 instance i-123456 for 5 minutes
aotutil ssm wait-patch i-1234356 --timeout 5m`,
		Run: func(cmd *cobra.Command, args []string) {
			instance := oneInstanceId(args)
			if err := ssmWrapper.WaitPatch(ctx, instance, ssmWaitTimeout); err != nil {
				logger.Fatal("WaitPatch failed", zap.Error(err))
			}
			logger.Info("WaitPatch done")
		},
	}

	// Wait Patch Report
	watchPatchReport := cobra.Command{
		Use:   "wait-patch-report",
		Short: "Wait until a patched instance is reported",
		Run: func(cmd *cobra.Command, args []string) {
			instance := oneInstanceId(args)
			if err := ssmWrapper.WaitPatchReported(ctx, instance, ssmWaitTimeout); err != nil {
				logger.Fatal("WaitPatch failed", zap.Error(err))
			} else {

			}
			logger.Info("WaitPatch done")
		},
	}

	root.AddCommand(
		&waitPatch,
		&watchPatchReport,
	)
	return &root
}
