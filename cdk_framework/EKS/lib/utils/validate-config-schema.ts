//install using npm install ajv
import Ajv, {DefinedError} from "ajv"

const ajv = new Ajv({allErrors: true});
require("ajv-errors")(ajv /*, {singleError: true} */)

const schema = {
    type: "object",
    properties: {
        clusters : {
            type: "array",
            items: {
                type: "object",
                properties: {
                    name: {
                        type: "string",
                        errorMessage: {
                            type: 'Name must be a string'
                        }
                    },
                    version:  {
                        type: "string",
                        errorMessage: {
                            type: 'Version must be a string'
                        }
                    },
                    launch_type: {
                        type: "string",
                        errorMessage: {
                            type: 'launch_type must be a string'
                        }
                    },
                    ec2_instance:  {
                        type: "string",
                        errorMessage: {
                            type: 'ec2_instance must be a string'
                        }
                    },
                    node_size: {
                        type: "string",
                        errorMessage: {
                            type: 'Node_size must be a string'
                        }
                    },
                },
                required: ["name", "version", "launch_type"],
                additionalProperties: false
            },
            minItems: 1
        }
    }
}

export function validateFileSchema(configData: unknown){
    const valid = ajv.validate(schema, configData)
    if (!valid){
        const errors = ajv.errors as DefinedError[]
        for(const err of errors){
            throw new Error(err.message) 
        }
    } 
}