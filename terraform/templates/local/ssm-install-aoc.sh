#!/bin/bash

set -ue

instance_id=$1
package_name=$2
package_version=$3
parameter_name=$4

echo "Sleep 15s for EC2 become online in SSM."
sleep 15

for i in {1..30}; do
  output=$(aws ssm describe-instance-information --filters "Key=InstanceIds,Values=$instance_id")
  echo ${output}
  status=$(echo ${output} | python3 -c "import sys, json; print('online') if len(json.load(sys.stdin)['InstanceInformationList']) == 1 else print('down')")
  if [[ ${status} == "online" ]]
  then
    break
  else
    echo "Wait 5s for EC2 become online in SSM...""$i"
    sleep 5
  fi
done

for j in {1..3}; do
  output=$(aws ssm send-command \
    --document-name "AWS-ConfigureAWSPackage" \
    --document-version "1" \
    --targets '[{"Key":"InstanceIds","Values":["'$instance_id'"]}]' \
    --parameters '{"action":["Install"],"installationType":["Uninstall and reinstall"],"name":["'$package_name'"],"version":["'$package_version'"],"additionalArguments":["{\"SSM_CONFIG\": \"{{ssm:'$parameter_name'}}\"}"]}' \
    --timeout-seconds 600 \
    --max-concurrency "50" \
    --max-errors "0" \
    --region us-west-2)
  echo ${output}

  command_id=$(echo ${output} | python3 -c "import sys, json; print(json.load(sys.stdin)['Command']['CommandId'])")

  echo "Sleep 15s for SSM command execution."
  sleep 15

  for i in {1..60}; do
    output=$(aws ssm get-command-invocation \
        --command-id "$command_id" \
        --instance-id "$instance_id")
    echo ${output}
    status=$(echo ${output} | python3 -c "import sys, json; print(json.load(sys.stdin)['Status'])")
    if [[ ${status} == "Success" ]]
    then
      echo "Sleep 30s for ADOT Collector start..."
      sleep 30
      exit 0
    elif [[ ${status} == "Failed" ]]
    then
      break
    else
      echo "Wait 5s for SSM command complete...""$i"
      sleep 5
    fi
  done

  echo "SSM installation failed. Try again...""$j"
done


