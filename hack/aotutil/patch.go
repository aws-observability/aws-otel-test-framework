package main

import "time"

const (
	SSMPatchDocument  = "AWS-RunPatchBaseline"
	SSMReportDocument = "AWS-GatherSoftwareInventory"
)

type EC2PatchStatus struct {
	PatchTime  time.Time
	ReportTime time.Time
}

type DescribeInstanceAssociationStatusResponse struct {
	InstanceAssociationStatusInfos []InstanceAssociationStatusInfo `json:"InstanceAssociationStatusInfos"`
}

type InstanceAssociationStatusInfo struct {
	AssociationID      string    `json:"AssociationId"`
	Name               string    `json:"Name"`
	DocumentVersion    string    `json:"DocumentVersion"`
	AssociationVersion string    `json:"AssociationVersion"`
	InstanceID         string    `json:"InstanceId"`
	ExecutionDate      time.Time `json:"ExecutionDate"`
	Status             string    `json:"Status"`
	ExecutionSummary   string    `json:"ExecutionSummary,omitempty"`
	AssociationName    string    `json:"AssociationName"`
	DetailedStatus     string    `json:"DetailedStatus,omitempty"`
}
