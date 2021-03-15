package main

import (
	"encoding/json"
	"fmt"
	"log"
	"os/exec"
)

func main() {
	//s, err := isInstancePatchReported("i-0e7af2bb70fa81a01")
	s, err := isInstancePatchReported("i-0fe4a514f1e6346e1")
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("%v", s)
}

// TODO: merge logic from check patch

func isInstancePatchReported(instanceId string) (*EC2PatchStatus, error) {
	cmd := exec.Command("aws", "ssm", "describe-instance-associations-status", "--instance-id", instanceId)
	b, err := cmd.CombinedOutput()
	if err != nil {
		return nil, fmt.Errorf("describe instance association status failed id=%s : %w", instanceId, err)
	}
	var res DescribeInstanceAssociationStatusResponse
	if err := json.Unmarshal(b, &res); err != nil {
		return nil, fmt.Errorf("decode describe isntance association status response failed: %w", err)
	}
	var status EC2PatchStatus
	for _, assoc := range res.InstanceAssociationStatusInfos {
		switch assoc.Name {
		case SSMPatchDocument:
			status.PatchTime = assoc.ExecutionDate
		case SSMReportDocument:
			status.ReportTime = assoc.ExecutionDate
		}
	}
	if status.PatchTime.IsZero() {
		return &status, fmt.Errorf("patch time not found")
	}
	if status.ReportTime.IsZero() {
		return &status, fmt.Errorf("report time not found")
	}
	if status.ReportTime.Before(status.PatchTime) {
		return &status, fmt.Errorf("report time is ealier than patch time report=%s patch=%s", status.ReportTime, status.PatchTime)
	}
	return &status, nil
}
