import { ClusterInterface } from '../../interfaces/eks/cluster-interface';
import { ec2ClusterInterface } from '../../interfaces/eks/ec2cluster-interface';

const validateSchema = require('yaml-schema-validator');

const supportedLaunchTypes = new Set(['fargate', 'ec2']);
const supportedVersions = new Set(['1.24', '1.25', '1.26', '1.27']);
const supportedCPUArchitectures = new Set(['m5', 'm6g', 't4g']);
const supportedNodeSizes = new Set([
  'medium',
  'large',
  'xlarge',
  '2xlarge',
  '4xlarge',
  '8xlarge',
  '12xlarge',
  '16xlarge',
  '24xlarge',
  'metal'
]);
const supportedT4gInstances = new Set([
  'nano',
  'micro',
  'small',
  'medium',
  'large',
  'xlarge',
  '2xlarge'
]);

const requiredSchema = {
  name: {
    type: String,
    required: true
  },
  version: {
    type: String,
    required: true,
    use: { validateVersion }
  },
  launch_type: {
    type: String,
    required: true,
    use: { checkLaunchType }
  },
  instance_type: {
    type: String,
    use: { validateInstanceType }
  },
  cert_manager: {
    type: Boolean
  }
};

export function validateInterface(
  cluster: ec2ClusterInterface | ClusterInterface
) {
  const error = validateSchema(cluster, { schema: requiredSchema });
  if (error === undefined || error.length !== 0) {
    throw new Error(
      'There was an error in configuration: Check Red Message above'
    );
  }
}

function checkLaunchType(val: string) {
  const adjustedType = val.toLowerCase();
  if (!supportedLaunchTypes.has(adjustedType)) {
    throw new Error('');
  }
  return adjustedType;
}

function validateVersion(version: string) {
  if (!supportedVersions.has(version)) {
    throw new Error(
      `Version needs to be a value of one of the following: ${Array.from(
        supportedVersions
      ).join(', ')}`
    );
  }
  return version;
}

function validateInstanceType(instance: string) {
  const splitted = instance.split('.');
  if (splitted.length !== 2) {
    throw new Error(
      'Instace_type is not in required /"ec2_instance.node_size/" template'
    );
  }
  const adjustedInstance = validateEC2Instance(splitted[0]);
  validateNodeSize(splitted[1], adjustedInstance);
  return true;
}

function validateEC2Instance(instance: string) {
  const adjustedType = instance.toLowerCase();
  if (!supportedCPUArchitectures.has(adjustedType)) {
    throw new Error(
      'Improper instance type or provided faulty ec2_instance/node_size for fargate cluster'
    );
  }
  return adjustedType;
}

function validateNodeSize(size: string, instance: string) {
  const adjustedSize = size.toLowerCase();

  if (instance === 't4g') {
    if (!supportedT4gInstances.has(adjustedSize)) {
      throw new Error(
        'Node size is not one of the options listed here https://www.amazonaws.cn/en/ec2/instance-types/'
      );
    }
  } else {
    if (!supportedNodeSizes.has(adjustedSize)) {
      throw new Error(
        'Node size is not one of the options listed here https://www.amazonaws.cn/en/ec2/instance-types/'
      );
    }
    if (instance === 'm5' && adjustedSize === 'medium') {
      throw new Error(
        'CPU architecture and node size are not compatible. Check here for compatibility options: https://www.amazonaws.cn/en/ec2/instance-types/'
      );
    }
    if (instance === 'm6g' && adjustedSize === '24xlarge') {
      throw new Error(
        'CPU architecture and node size are not compatible. Check here for compatibility options: https://www.amazonaws.cn/en/ec2/instance-types/'
      );
    }
  }

  return adjustedSize;
}
