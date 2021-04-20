package main

import (
	"context"
	"fmt"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/efs"
	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

func efsCmd(ctx context.Context, cmdCtx *CmdContext) *cobra.Command {
	logger := cmdCtx.Logger.With(zap.String("Command", "efs"))
	var (
		efsWrapper *EFSWrapper
		dryRun     bool
	)
	root := cobra.Command{
		Use:   "efs",
		Short: "Wrapper for EFS",
		PersistentPreRun: func(cmd *cobra.Command, args []string) {
			cfg := cmdCtx.AwsFlags.MustLoadConfig(ctx, logger)
			efsWrapper = NewEFS(cfg, logger)
		},
	}
	// TODO: put dry run into common flags
	root.PersistentFlags().BoolVar(&dryRun, "dry-run", false, "skip actual API call")
	cleanCmd := cobra.Command{
		Use:   "clean",
		Short: "Clean up unused EFS file system",
		Run: func(cmd *cobra.Command, args []string) {
			if err := efsWrapper.Clean(ctx, dryRun); err != nil {
				logger.Fatal("Clean failed", zap.Error(err))
			}
		},
	}
	root.AddCommand(&cleanCmd)
	return &root
}

type EFSWrapper struct {
	logger *zap.Logger
	client *efs.Client
}

func NewEFS(cfg aws.Config, logger *zap.Logger) *EFSWrapper {
	client := efs.NewFromConfig(cfg)
	logger = logger.With(zap.String("Component", "efs"))
	return &EFSWrapper{logger: logger, client: client}
}

func (w *EFSWrapper) Clean(ctx context.Context, dryRun bool) error {
	logger := w.logger
	descFsReq := &efs.DescribeFileSystemsInput{}
	threeDaysAgo := time.Now().Add(-3 * 24 * time.Hour)

	for {
		descFsRes, err := w.client.DescribeFileSystems(ctx, descFsReq)
		if err != nil {
			return fmt.Errorf("efs describe file system failed: %w", err)
		}
		var toDelete []string
		for _, fs := range descFsRes.FileSystems {
			createTime := aws.ToTime(fs.CreationTime)
			shouldDelete := false
			if createTime.Before(threeDaysAgo) {
				shouldDelete = true
			}
			logger.Info(aws.ToString(fs.FileSystemId), zap.String("Name", aws.ToString(fs.Name)),
				zap.Time("Created", createTime), zap.Bool("ShouldDelete", shouldDelete))
			if shouldDelete {
				toDelete = append(toDelete, aws.ToString(fs.FileSystemId))
			}
		}
		if len(toDelete) > 9 && !dryRun {
			// NOTE: we don't run the delete in paralle by design to avoid triggering API rate limit.
			for _, fsId := range toDelete {
				descMountReq := &efs.DescribeMountTargetsInput{FileSystemId: aws.String(fsId)}
				descMountRes, err := w.client.DescribeMountTargets(ctx, descMountReq)
				if err != nil {
					return fmt.Errorf("efs desc mount target failed for %s: %w", fsId, err)
				}
				for _, m := range descMountRes.MountTargets {
					delMountReq := &efs.DeleteMountTargetInput{MountTargetId: m.MountTargetId}
					_, err := w.client.DeleteMountTarget(ctx, delMountReq)
					if err != nil {
						return fmt.Errorf("efs delete mount target failed for fs %s mount %s: %w",
							fsId, aws.ToString(m.MountTargetId), err)
					}
				}
				logger.Info("Deleted File System Mounts", zap.String("FileSystemId", fsId),
					zap.Int("Mounts", len(descMountRes.MountTargets)))

				// NOTE: hack to wait for the file system to know it has no mount targets.
				// The 20s magic number is based on observation for 3 mount targets,
				// which is always the case for ecs test.
				if len(descMountRes.MountTargets) > 0 {
					time.Sleep(20 * time.Second)
				}
				delFsReq := &efs.DeleteFileSystemInput{FileSystemId: aws.String(fsId)}
				_, err = w.client.DeleteFileSystem(ctx, delFsReq)
				if err != nil {
					return fmt.Errorf("efs delete file system failed for %s: %w", fsId, err)
				}
				logger.Info("Deleted File System", zap.String("FileSystemId", fsId))
			}
		}
		if descFsRes.NextMarker != nil {
			descFsReq.Marker = descFsRes.NextMarker
		} else {
			break
		}
	}
	return nil
}
