package main

import (
	"context"
	"fmt"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/ec2"
	"github.com/aws/aws-sdk-go-v2/service/ec2/types"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

func ec2Cmd(ctx context.Context, cmdCtx *CmdContext) *cobra.Command {
	logger := cmdCtx.Logger.With(zap.String("Command", "ec2"))
	var (
		ec2Wrapper *EC2Wrapper
		dryRun     bool
	)
	root := cobra.Command{
		Use:   "ec2",
		Short: "Wrapper for EC2",
		PersistentPreRun: func(cmd *cobra.Command, args []string) {
			cfg := cmdCtx.AwsFlags.MustLoadConfig(ctx, logger)
			ec2Wrapper = NewEC2(cfg, logger)
		},
	}
	root.PersistentFlags().BoolVar(&dryRun, "dry-run", false, "skip actual API call")
	cleanCmd := cobra.Command{
		Use:   "clean",
		Short: "Clean up dangling ec2 test instances",
		Run: func(cmd *cobra.Command, args []string) {
			if err := ec2Wrapper.Clean(ctx, dryRun); err != nil {
				logger.Fatal("Clean failed", zap.Error(err))
			}
		},
	}

	root.AddCommand(&cleanCmd)
	return &root
}

type EC2Wrapper struct {
	logger *zap.Logger
	client *ec2.Client
}

func NewEC2(cfg aws.Config, logger *zap.Logger) *EC2Wrapper {
	client := ec2.NewFromConfig(cfg)
	logger = logger.With(zap.String("Component", "ec2"))
	return &EC2Wrapper{logger: logger, client: client}
}

func (e2 *EC2Wrapper) Clean(ctx context.Context, dryRun bool) error {
	req := &ec2.DescribeInstancesInput{}
	threeHoursAgo := time.Now().Add(-3 * time.Hour)
	for {
		res, err := e2.client.DescribeInstances(ctx, req)
		if err != nil {
			return fmt.Errorf("ec2 describe instance failed: %w", err)
		}
		var toTerminates []string
		for _, resv := range res.Reservations {
			for _, ins := range resv.Instances {
				// Ignore non running instance
				if ins.State.Name != types.InstanceStateNameRunning {
					continue
				}
				// TODO: we can't distinguish canary and soaking instances as the have same tag
				asg := ""
				name := ""
				for _, tag := range ins.Tags {
					v := aws.ToString(tag.Value)
					switch aws.ToString(tag.Key) {
					case "Name":
						name = v
					case "aws:autoscaling:groupName":
						asg = v
					}
				}
				shouldTerminate := asg == "" && (name == "Integ-test-Sample-App" || name == "Integ-test-aoc") &&
					aws.ToTime(ins.LaunchTime).Before(threeHoursAgo)
				e2.logger.Info(aws.ToString(ins.InstanceId),
					zap.String("Name", name), zap.String("ASG", asg), zap.Bool("ShouldTerminate", shouldTerminate))
				if shouldTerminate {
					toTerminates = append(toTerminates, aws.ToString(ins.InstanceId))
				}
			}
		}
		e2.logger.Info("Should terminate instances", zap.Int("Count", len(toTerminates)))
		if len(toTerminates) > 0 && !dryRun {
			termReq := &ec2.TerminateInstancesInput{
				InstanceIds: toTerminates,
			}
			res, err := e2.client.TerminateInstances(ctx, termReq)
			if err != nil {
				return fmt.Errorf("terminate instances failed: %w", err)
			}
			e2.logger.Info("Terminate instance request sent", zap.Int("Count", len(res.TerminatingInstances)))
		}

		if res.NextToken != nil {
			req.NextToken = res.NextToken
		} else {
			break
		}
	}
	return nil
}
