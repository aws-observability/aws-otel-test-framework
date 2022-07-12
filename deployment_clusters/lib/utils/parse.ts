import { singletonEventRole } from 'aws-cdk-lib/aws-events-targets';
import { FunctionVersionUpgrade } from 'aws-cdk-lib/aws-lambda';
import { readFileSync, writeFileSync } from 'fs';
import { exit } from 'process';
const yaml = require('js-yaml')

const supportedVersions = new Set(['1.18', '1.19', '1.20', '1.21', '1.22']);
const supportedCPUArchitectures = new Set(['amd_64', 'arm_64']);
const supportedLaunchTypes = new Set(['ec2', 'fargate']);
const supportedNodeSizes = new Set(['medium', 'large', 'xlarge', '2xlarge', '4xkarge', '8xlarge', '12xlarge', '16xlarge', '24xlarge', 'metal']);

export function parseData(info: Object){
    var bigMap = new Map()
    for(const [key, value] of Object.entries(info)){
        // console.log(key,value)
        const valString = JSON.stringify(value)
        const valArray = valString.split(',')
        // console.log(v)
        var dict = new Map();
        for(var index in valArray){
          const st = valArray[index]
          const fixedString = st.replace("{", "").replace(/"/g, '').replace("}", "");
          valArray[index] = fixedString
          const fixedStringArray = fixedString.split(':')
          dict.set(fixedStringArray[0], fixedStringArray[1])
        }
        const cpuType = dict.get('cpu_architecture')
        const fixedCPUType = refactorAndValidateArchitecture(cpuType)
        dict.set('cpu_architecture', fixedCPUType);
        const launchType = dict.get('launch_type')
        const fixedLaunchType = refactorAndValidateLaunchType(launchType)
        dict.set('cpu_architecture', fixedLaunchType);
        validateVersion(dict.get('version'))
        // console.log(dict);
        // console.log(dict.get('version'))
        bigMap.set(key, dict);
    }
    return bigMap
}

export function validateClusters(info: Object){
    for(const [key, value] of Object.entries(info)){
        // console.log(key,value)
        const val = Object(value)
        for(const [k, v] of Object.entries(val)){
            switch(k){
                case 'version':
                    val[k] = validateVersion(String(v))
                    break;
                case 'cpu_architecture':
                    val[k] = refactorAndValidateArchitecture(String(v))
                    break
                case 'launch_type':
                    val[k] = refactorAndValidateLaunchType(String(v))
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
        version = "1.20"
    }

    // const supportedVersions = new Set(['1.18', '1.19', '1.20', '1.21', '1.22']);
    if(!supportedVersions.has(version)){
        throw new Error("Version needs to be number between 1.18 to 1.22");
    }
    return version
}

function refactorAndValidateArchitecture(cpu: string){
    if(cpu === null || !cpu || cpu == 'null'){
        console.log("It is null: " + cpu)
        return null
    }
    const adjustedType = cpu.toLowerCase()
    if(!supportedCPUArchitectures.has(adjustedType)){
        throw new Error("Improper CPU Architecture Type")
    }
    return adjustedType
}

function refactorAndValidateLaunchType(type: string){
    if(type == null){
        throw new Error("Launch Type can't be null")
    }
    // const supportedLaunchTypes = new Set(['ec2', 'fargate']);
    const adjustedType = type.toLowerCase().replace(/[\W_]+/g, "");
    if(!supportedLaunchTypes.has(adjustedType)){
        throw new Error("Improper CPU Architecture Type")
    }
    // if(adjustedType != 'ec2' && adjustedType != 'fargate'){
    //     throw new Error("Improper CPU Architecture Type")
    // }
    return adjustedType
}

function validateNodeSize(size: string){
    if(!size || size === null || size === 'null' || size === ''){
        return null
    }
    const adjustedSize = size.toLowerCase()
    if(!supportedNodeSizes.has(adjustedSize)){
        throw new Error("Node size isn't one of the options listed here https://www.amazonaws.cn/en/ec2/instance-types/")
    }
    return adjustedSize

}

function addedChecks(val: Object){
    const value = Object(val)
    if(String(value['launch_type']) === 'ec2' && !supportedCPUArchitectures.has(String(value['cpu_architecture']))){
        throw new Error('ec2 type needs to have cpu architecture type')
    }
    if(String([value['cpu_architecture']]) === 'arm_66' && String([value['node_size']]) === '24xlarge'){
        throw new Error("Cpur architecture and node size aren't compatible")
    }
}