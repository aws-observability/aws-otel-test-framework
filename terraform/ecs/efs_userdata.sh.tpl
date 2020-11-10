#! /bin/bash

# mount efs
sudo mkdir -p /efs
sudo yum install amazon-efs-utils -y
sudo mount -t efs ${efs_id}:/ /efs

## add root cert
(
cat << EOF
${cert_content}
EOF
) | tee /efs/ca-bundle.crt
