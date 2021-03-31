# AWS Patch

## Overview

For security reason, some AWS accounts requires installing latest patch for all ec2 instances and report the status
after patch is done.

## Design

The patching has the following requirements (which we didn't set it up using terraform)

- IAM role with ssm policy `arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore`
- AMI with ssm agent (which can also be installed manually using userdata)
- ssm association that runs patch and report
    - the minimal interval is 30 minutes, but it always run immediately when instance first starts
    - report runs BEFORE patch, so you have to wait at least 30 minutes before terminating the instance

## Implementation

### EC2 Patch

- Patch might reboot the instance so we only start installing collector etc. after it is already rebooted.
- Most EC2 instance are short term instances we need to make sure they run long enough to report they are patched.

We use a null resource, when creating it calls `wait-patch` , when it got destroyed the second provision is called and
blocks until the patch is reported.

```tf
resource "null_resource" "check_patch" {
  depends_on = [
    aws_instance.aoc,
    aws_instance.sidecar]
  count = var.patch ? 1 : 0

  # https://discuss.hashicorp.com/t/how-to-rewrite-null-resource-with-local-exec-provisioner-when-destroy-to-prepare-for-deprecation-after-0-12-8/4580/2
  triggers = {
    sidecar_id = aws_instance.sidecar.id
    aoc_id = aws_instance.aoc.id
    aotutil = "../../hack/aotutil/aotutil"
  }

  provisioner "local-exec" {
    command = <<-EOT
     "${self.triggers.aotutil}" ssm wait-patch "${self.triggers.sidecar_id}"
     "${self.triggers.aotutil}" ssm wait-patch "${self.triggers.aoc_id}"
    EOT
  }

  provisioner "local-exec" {
    when = destroy
    command = <<-EOT
      "${self.triggers.aotutil}" ssm wait-patch-report "${self.triggers.sidecar_id}"
      "${self.triggers.aotutil}" ssm wait-patch-report "${self.triggers.aoc_id}"
    EOT
  }
}
```

### ECS EC2 Patch

- [ ] it ships w/ ssm by default but might need to check IAM and tag

### EKS EC2 Patch

- eks cluster is created manually and has deployed the following daemonset to install SSM agent.

NOTE: this only works for distro that uses rpm, for apt based like Debian and Ubuntu, you need to adjust the command in
initContainers to add install command to host's cron.

```yaml
# NOTE: this modified version of https://docs.aws.amazon.com/prescriptive-guidance/latest/patterns/install-ssm-agent-on-amazon-eks-worker-nodes-by-using-kubernetes-daemonset.html
# - kind is changed to apps/v1 from extensions/v1beta1
# - don't remove rpm rpm -e amazon-ssm-agent-2.3.1550.0-1.x86_64
apiVersion: apps/v1
#apiVersion: extensions/v1beta1 # NOTE: use this one if k8s version < 1.7
kind: DaemonSet
metadata:
  labels:
    k8s-app: ssm-installer
  name: ssm-installer
  namespace: default
spec:
  selector:
    matchLabels:
      k8s-app: ssm-installer
  template:
    metadata:
      labels:
        k8s-app: ssm-installer
    spec:
      initContainers:
        - image: public.ecr.aws/amazonlinux/amazonlinux:latest
          name: ssm
          command: [ "/bin/bash","-c","echo '* * * * * root yum install -y https://s3.amazonaws.com/ec2-downloads-windows/SSMAgent/latest/linux_amd64/amazon-ssm-agent.rpm && systemctl restart amazon-ssm-agent && rm -rf /etc/cron.d/ssmstart' > /etc/cron.d/ssmstart && echo 'Successfully installed SSM agent'" ]
          securityContext:
            allowPrivilegeEscalation: true
          volumeMounts:
            - mountPath: /etc/cron.d
              name: cronfile
      containers:
        - image: public.ecr.aws/eks-distro/kubernetes/pause:v1.18.9-eks-1-18-1
          name: pause
      volumes:
        - name: cronfile
          hostPath:
            path: /etc/cron.d
            type: Directory
      restartPolicy: Always
```