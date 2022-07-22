const yaml = require('js-yaml')

const supportedFields = new Set(['version', 'cpu_architecture', 'launch_type', 'node_size'])
const supportedVersions = new Set(['1.18', '1.19', '1.20', '1.21']);
const supportedCPUArchitectures = new Set(['amd_64', 'arm_64']);
const supportedLaunchTypes = new Set(['ec2', 'fargate']);
const supportedNodeSizes = new Set(['medium', 'large', 'xlarge', '2xlarge', '4xkarge', '8xlarge', '12xlarge', '16xlarge', '24xlarge', 'metal']);


export function validateClustersConfig(info: Object){
    const data = Object(info)
    if(!data['clusters']){
        throw new Error('No clusters field being filed in the yaml file')
    }
    const clusterInfo = data['clusters']
    for(const [key, value] of Object.entries(clusterInfo)){
        
        const val = Object(value)
        if(Object.keys(val).length !== 4){
            throw new Error("Didn't set all the fields for the clusters")
        }
        for(const [k, v] of Object.entries(val)){
            if(!supportedFields.has(k)){
                throw new Error("Uncompatible field type")
            }
            switch(k){
                case 'version':
                    val[k] = validateVersion(String(v))
                    break;
                case 'cpu_architecture':
                    val[k] = convertAndValidateArchitecture(String(v))
                    break
                case 'launch_type':
                    val[k] = convertAndValidateLaunchType(String(v))
                    break
                case 'node_size':
                    val[k] = validateNodeSize(String(v))
                    break;
            }
        }
        addedChecks(val)
    }
}



function validateVersion(version: string){
    
    if(version === '1.2'){
        version = '1.20'
    }

    if(!supportedVersions.has(version)){
        throw new Error('Version needs to be number between 1.18 to 1.21');
    }
    return version
}

function convertAndValidateArchitecture(cpu: string){
    if(cpu === null || !cpu || cpu == 'null'){
        console.log('It is null: ' + cpu)
        return null
    }
    const adjustedType = cpu.toLowerCase()
    if(!supportedCPUArchitectures.has(adjustedType)){
        throw new Error('Improper CPU Architecture Type')
    }
    return adjustedType
}

function convertAndValidateLaunchType(type: string){
    if(type == null){
        throw new Error("Launch Type can't be null")
    }
   
    const adjustedType = type.toLowerCase().replace(/[\W_]+/g, "");
    if(!supportedLaunchTypes.has(adjustedType)){
        throw new Error('Improper CPU Architecture Type')
    }
    return adjustedType
}

function validateNodeSize(size: string){
    if(!size || size === null || size === 'null' || size === ''){
        return null
    }
    const adjustedSize = size.toLowerCase()
    if(!supportedNodeSizes.has(adjustedSize)){
        throw new Error('Node size is not one of the options listed here https://www.amazonaws.cn/en/ec2/instance-types/')
    }
    return adjustedSize

}

function addedChecks(val: Object){
    const value = Object(val)
    if(String(value['launch_type']) === 'ec2' && !supportedCPUArchitectures.has(String(value['cpu_architecture']))){
        throw new Error('ec2 type needs to have cpu architecture type')
    }
    if(String([value['cpu_architecture']]) === 'arm_64' && String([value['node_size']]) === '24xlarge'){
        throw new Error("Cpur architecture and node size aren't compatible")
    }

    if(String([value['cpu_architecture']]) === 'amd_64' && String([value['node_size']]) === 'medium'){
        throw new Error("Cpur architecture and node size aren't compatible")
    }
}