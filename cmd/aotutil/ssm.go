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
	"strings"
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
	root.PersistentFlags().DurationVar(&ssmWaitTimeout, "timeout", 25*time.Minute, "abort polling if timeout duration is exceeded")
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

	// Run Command
	var document string
	runCommand := cobra.Command{
		Use:   "run-command INSTANCE_ID -- COMMAND [COMMAND...]",
		Short: "Run shell commands on an instance via SSM and stream output",
		Example: `# Run a single command on a Linux instance
aotutil ssm run-command i-123456 -- "sudo systemctl start aws-otel-collector"
# Run multiple commands
aotutil ssm run-command i-123456 -- "cd /tmp" "sudo ./install.sh"
# Run on Windows (auto-detected, or force with --document)
aotutil ssm run-command i-123456 --document AWS-RunPowerShellScript -- "Get-Service"`,
		Args: cobra.MinimumNArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			instance := args[0]
			commands := args[1:]
			if len(commands) == 0 {
				logger.Fatal("No commands provided after instance ID (use -- separator)")
			}
			output, err := ssmWrapper.RunCommand(ctx, instance, document, commands, ssmWaitTimeout)
			if err != nil {
				logOrIgnore("RunCommand failed", err)
			} else {
				if output != "" {
					fmt.Print(output)
				}
				logger.Info("RunCommand completed successfully")
			}
		},
	}
	runCommand.Flags().StringVar(&document, "document", "", "SSM document (default: auto-detect Linux/Windows)")

	root.AddCommand(
		&waitPatch,
		&watchPatchReport,
		&runCommand,
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

func (s *SSMWrapper) RunCommand(ctx context.Context, instanceId, document string, commands []string, timeout time.Duration) (string, error) {
	logger := s.logger.With(zap.String("InstanceId", instanceId), zap.String("Action", "RunCommand"))

	// Wait for instance to appear in SSM
	logger.Info("Waiting for instance to be online in SSM")
	if err := s.waitOnline(ctx, instanceId, 5*time.Minute); err != nil {
		return "", fmt.Errorf("instance not online in SSM: %w", err)
	}

	// Auto-detect document if not specified
	if document == "" {
		platform, err := s.detectPlatform(ctx, instanceId)
		if err != nil {
			logger.Warn("Could not detect platform, defaulting to Linux", zap.Error(err))
			document = "AWS-RunShellScript"
		} else if strings.EqualFold(platform, "Windows") {
			document = "AWS-RunPowerShellScript"
		} else {
			document = "AWS-RunShellScript"
		}
	}
	logger.Info("Sending command", zap.String("Document", document), zap.Int("NumCommands", len(commands)))

	// Send command
	sendRes, err := s.client.SendCommand(ctx, &ssm.SendCommandInput{
		InstanceIds:  []string{instanceId},
		DocumentName: aws.String(document),
		Parameters:   map[string][]string{"commands": commands},
		TimeoutSeconds: int32(timeout.Seconds()),
	})
	if err != nil {
		return "", fmt.Errorf("send command failed: %w", err)
	}
	commandId := aws.ToString(sendRes.Command.CommandId)
	logger.Info("Command sent", zap.String("CommandId", commandId))

	// Poll for completion
	pollInterval := 5 * time.Second
	deadline := time.Now().Add(timeout)
	for {
		if time.Now().After(deadline) {
			return "", fmt.Errorf("command timed out after %s (CommandId: %s)", timeout, commandId)
		}
		time.Sleep(pollInterval)

		invRes, err := s.client.GetCommandInvocation(ctx, &ssm.GetCommandInvocationInput{
			CommandId:  aws.String(commandId),
			InstanceId: aws.String(instanceId),
		})
		if err != nil {
			// InvocationDoesNotExist is expected briefly after send
			if strings.Contains(err.Error(), "InvocationDoesNotExist") {
				continue
			}
			return "", fmt.Errorf("get command invocation failed: %w", err)
		}

		status := invRes.Status
		switch status {
		case types.CommandInvocationStatusSuccess:
			output := aws.ToString(invRes.StandardOutputContent)
			logger.Info("Command succeeded", zap.String("CommandId", commandId))
			return output, nil
		case types.CommandInvocationStatusFailed, types.CommandInvocationStatusCancelled, types.CommandInvocationStatusTimedOut:
			stdout := aws.ToString(invRes.StandardOutputContent)
			stderr := aws.ToString(invRes.StandardErrorContent)
			combined := ""
			if stdout != "" {
				combined += "STDOUT:\n" + stdout + "\n"
			}
			if stderr != "" {
				combined += "STDERR:\n" + stderr + "\n"
			}
			if combined != "" {
				fmt.Print(combined)
			}
			return "", fmt.Errorf("command %s (CommandId: %s, StatusDetails: %s)",
				status, commandId, aws.ToString(invRes.StatusDetails))
		default:
			logger.Debug("Command still running", zap.String("Status", string(status)))
		}
	}
}

func (s *SSMWrapper) waitOnline(ctx context.Context, instanceId string, timeout time.Duration) error {
	deadline := time.Now().Add(timeout)
	for {
		if time.Now().After(deadline) {
			return fmt.Errorf("timeout waiting for instance %s to appear in SSM", instanceId)
		}
		res, err := s.client.DescribeInstanceInformation(ctx, &ssm.DescribeInstanceInformationInput{
			Filters: []types.InstanceInformationStringFilter{
				{Key: aws.String("InstanceIds"), Values: []string{instanceId}},
			},
		})
		if err == nil && len(res.InstanceInformationList) > 0 {
			ping := res.InstanceInformationList[0].PingStatus
			if ping == types.PingStatusOnline {
				return nil
			}
		}
		time.Sleep(5 * time.Second)
	}
}

func (s *SSMWrapper) detectPlatform(ctx context.Context, instanceId string) (string, error) {
	res, err := s.client.DescribeInstanceInformation(ctx, &ssm.DescribeInstanceInformationInput{
		Filters: []types.InstanceInformationStringFilter{
			{Key: aws.String("InstanceIds"), Values: []string{instanceId}},
		},
	})
	if err != nil {
		return "", err
	}
	if len(res.InstanceInformationList) == 0 {
		return "", fmt.Errorf("instance not found in SSM")
	}
	return string(res.InstanceInformationList[0].PlatformType), nil
}
