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
	"fmt"
	"time"

	"github.com/spf13/cobra"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/ssm"
	"github.com/aws/aws-sdk-go-v2/service/ssm/types"
	"go.uber.org/zap"
)

const (
	SSMPatchDocument  = "AWS-RunPatchBaseline"
	SSMReportDocument = "AWS-GatherSoftwareInventory"
)

const (
	waitInterval                  = time.Minute
	waitPatchReportMinimalTimeout = 30 * time.Minute // this is the minimal ssm association interval
)

type SSMWrapper struct {
	logger *zap.Logger
	client *ssm.Client
}

func ssmCmd(ctx context.Context, cmdCtx *CmdContext) *cobra.Command {
	logger := cmdCtx.Logger.With(zap.String("Command", "ssm"))
	var (
		ssmWrapper     *SSMWrapper
		ssmWaitTimeout time.Duration
		ignoreError    bool
	)
	root := cobra.Command{
		Use:   "ssm",
		Short: "Wrapper for SSm (AWS Systems Manager)",
		PersistentPreRun: func(cmd *cobra.Command, args []string) {
			cfg := cmdCtx.AwsFlags.MustLoadConfig(ctx, logger)
			ssmWrapper = NewSSM(cfg, logger)
		},
	}
	root.PersistentFlags().DurationVar(&ssmWaitTimeout, "timeout", 15*time.Minute, "abort polling if timeout duration is exceeded")
	root.PersistentFlags().BoolVar(&ignoreError, "ignore-error", false, "exit 0 when patch/report failed")

	oneInstanceId := func(args []string) string {
		if len(args) == 0 {
			logger.Fatal("Instance ID is required as position arguments")
		}
		if len(args) > 1 {
			logger.Warn("Only one instance ID is supported", zap.Int("Got", len(args)))
		}
		return args[0]
	}

	logOrIgnore := func(msg string, err error) {
		if ignoreError {
			logger.Warn(msg, zap.Error(err))
		} else {
			logger.Fatal(msg, zap.Error(err))
		}
	}

	// Wait Patch
	waitPatch := cobra.Command{
		Use:   "wait-patch",
		Short: "Wait until patch association is completed or timeout",
		Example: `# Wait patch on ec2 instance i-123456 for 5 minutes
aotutil ssm wait-patch i-1234356 --timeout 5m
# Wait but ignore error
aotutil ssm wait-patch i-1234356 --ignore-error`,
		Run: func(cmd *cobra.Command, args []string) {
			instance := oneInstanceId(args)
			if err := ssmWrapper.WaitPatch(ctx, instance, ssmWaitTimeout); err != nil {
				logOrIgnore("WatPatch failed", err)
			} else {
				logger.Info("WaitPatch done")
			}
		},
	}

	// Wait Patch Report
	watchPatchReport := cobra.Command{
		Use:   "wait-patch-report",
		Short: "Wait until a patched instance is reported",
		Run: func(cmd *cobra.Command, args []string) {
			instance := oneInstanceId(args)
			if err := ssmWrapper.WaitPatchReported(ctx, instance, ssmWaitTimeout); err != nil {
				logOrIgnore("WaitPatchReport failed", err)
			} else {
				logger.Info("WaitPatch done")
			}
		},
	}

	root.AddCommand(
		&waitPatch,
		&watchPatchReport,
	)
	return &root
}

func NewSSM(cfg aws.Config, logger *zap.Logger) *SSMWrapper {
	client := ssm.NewFromConfig(cfg)
	logger = logger.With(zap.String("Component", "ssm"))
	return &SSMWrapper{logger: logger, client: client}
}

// NOTE: there is no builtin waiter implementation for checking association status.
func (s *SSMWrapper) WaitPatch(ctx context.Context, instanceId string, timeout time.Duration) error {
	logger := s.logger.With(zap.String("InstanceId", instanceId), zap.String("Action", "WaitPatch"))
	logger.Info("Start waiting patch")
	start := time.Now()
	return Wait(waitInterval, timeout, func() (WaitAction, error) {
		infos, err := describeInstanceAssocStatus(ctx, s.client, instanceId)
		if err != nil {
			return WaitDone, err
		}
		for _, assoc := range infos {
			if aws.ToString(assoc.Name) != SSMPatchDocument {
				continue
			}
			status := aws.ToString(assoc.Status)
			switch status {
			case "Success":
				logger.Info("patch on instance succeeded",
					zap.Time("PatchTime", aws.ToTime(assoc.ExecutionDate)),
					zap.Duration("Waited", time.Now().Sub(start)))
				return WaitDone, nil
			case "Failed":
				return WaitDone, fmt.Errorf("patch on instance failed instanceId %s waited %s", instanceId, time.Now().Sub(start))
			default:
				logger.Info("waiting patching", zap.String("Status", status))
				return WaitContinue, nil
			}
		}
		//return WaitDone, fmt.Errorf("patch document is not associated on the instance, requires %s", SSMPatchDocument)
		// NOTE: it seems it takes a while for SSM to associate the documents to instance ...
		return WaitContinue, nil
	})
}

func (s *SSMWrapper) WaitPatchReported(ctx context.Context, instanceId string, timeout time.Duration) error {
	logger := s.logger.With(zap.String("InstanceId", instanceId), zap.String("Action", "WaitPatchReported"))
	logger.Info("Start waiting patch report")
	// Force the minial wait time for patch report
	if timeout < waitPatchReportMinimalTimeout {
		timeout = waitPatchReportMinimalTimeout
	}
	return Wait(waitInterval, timeout, func() (WaitAction, error) {
		infos, err := describeInstanceAssocStatus(ctx, s.client, instanceId)
		if err != nil {
			return WaitDone, err
		}
		var patchTime, reportTime time.Time
		for _, assoc := range infos {
			status := aws.ToString(assoc.Status)
			switch status {
			case "Success":
				switch aws.ToString(assoc.Name) {
				case SSMPatchDocument:
					patchTime = aws.ToTime(assoc.ExecutionDate)
				case SSMReportDocument:
					reportTime = aws.ToTime(assoc.ExecutionDate)
				}
			}
		}
		logger.Info("waiting patch report", zap.Time("PatchTime", patchTime), zap.Time("ReportTime", reportTime))
		if patchTime.IsZero() || reportTime.IsZero() || reportTime.Before(patchTime) {
			return WaitContinue, nil
		}
		logger.Info("patch reported")
		return WaitDone, nil
	})
}

func describeInstanceAssocStatus(ctx context.Context, client *ssm.Client, instanceId string) ([]types.InstanceAssociationStatusInfo, error) {
	res, err := client.DescribeInstanceAssociationsStatus(ctx, &ssm.DescribeInstanceAssociationsStatusInput{
		InstanceId: aws.String(instanceId),
	})
	if err != nil {
		return nil, fmt.Errorf("describe instance association status failed: %w", err)
	}
	return res.InstanceAssociationStatusInfos, nil
}
