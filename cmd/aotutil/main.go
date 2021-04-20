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
		ec2Cmd(ctx, &cmdCtx),
		efsCmd(ctx, &cmdCtx),
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
