import assert from 'assert';
import { readFileSync} from 'fs';
import { validateClustersConfig } from '../lib/utils/validate';
const yaml = require('js-yaml')

const route = __dirname + '/test_config/test_clusters.yml';
const raw = readFileSync(route)
let data = yaml.load(raw)

beforeEach(() => {
    data = yaml.load(raw)
})


test('ValidationTest', () => {
    validateClustersConfig(data)
    assert(Object.keys(data['clusters']).length === 4)
    assert(data['clusters']['amdCluster']['launch_type']['ec2']['ec2_instance'] === 'm5')
});

test('Version Error', () => {
    data['clusters']['amdCluster']['version'] = 1.22
    try{
        validateClustersConfig(data)
        assert(false)
    } catch(error){
        expect(error).toBeInstanceOf(Error);
    }
})

test('Added cluster field Error', () => {
    data['clusters']['amdCluster']['addedKey'] = "random_value"
    try{
        validateClustersConfig(data)
        assert(false)
    } catch(error){
        expect(error).toBeInstanceOf(Error);
    }
})

test('Added launch_type field Error', () => {
    data['clusters']['t4gCluster']['launch_type']['ec2']['added_value'] = "random_value"
    try{
        validateClustersConfig(data)
        assert(false)
    } catch(error){
        expect(error).toBeInstanceOf(Error);
    }
})

test('2 launch_type fields Error', () => {
    data['clusters']['t4gCluster']['launch_type']['fargate'] = "addedField"
    try{
        validateClustersConfig(data)
        assert(false)
    } catch(error){
        expect(error).toBeInstanceOf(Error);
    }
})

test('Wrogn ec2 instance name Error', () => {
    data['clusters']['t4gCluster']['launch_type']['ec2']['ec2_instance'] = "wrong_type"
    try{
        validateClustersConfig(data)
        assert(false)
    } catch(error){
        expect(error).toBeInstanceOf(Error);
    }
})

test('Incompatible node size with t4g instance type Error', () => {
    data['clusters']['t4gCluster']['launch_type']['ec2']['node_size'] = "24xlarge"
    try{
        validateClustersConfig(data)
        assert(false)
    } catch(error){
        expect(error).toBeInstanceOf(Error);
    }
})

test('Incompatible node size with m6g instance type Error', () => {
    data['clusters']['armCluster']['launch_type']['ec2']['node_size'] = "24xlarge"
    try{
        validateClustersConfig(data)
        assert(false)
    } catch(error){
        expect(error).toBeInstanceOf(Error);
    }
})

