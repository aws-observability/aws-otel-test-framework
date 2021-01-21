#! /bin/bash

set -e

instance_id=$1

n=0
until [ "$n" -ge 20 ]
do
  aws ssm describe-instance-associations-status --instance-id "$instance_id" | grep "AWS-RunPatchBaseline" -A10 | grep "Success" && echo "patch finished" && break
  echo "waiting for patch, sleep 1 minute"
  n=$((n+1))
  sleep 60
done