#!/bin/bash

# Checks for cache hits for specific test case
# exits 1 if cache hit was missing
# required env vars
# DDB_TABLE_NAME: dyanmodb table name that will be queries
# TF_VAR_aoc_version: test image version that was used
# DDB_BATCH_CACHE_SK(OPTIONAL): If set then the prefix
# of the sortkey will be set to this value. The default value is the 
# value of TF_VAR_aoc_version. This is useful if testing
# something other than the ADOT Collector and you would like to use a different
# sort key. 

test_framework_shortsha=$(git rev-parse --short HEAD)

if [[ -z "${DDB_BATCH_CACHE_SK}" ]]; then
    DDB_SK_PREFIX=$TF_VAR_aoc_version
else
    DDB_SK_PREFIX=$DDB_BATCH_CACHE_SK
fi

CACHE_HIT=$(aws dynamodb get-item --region=us-west-2 --table-name ${DDB_TABLE_NAME} --key {\"TestId\":{\"S\":\"$1$2$3\"}\,\"aoc_version\":{\"S\":\"$DDB_SK_PREFIX$test_framework_shortsha\"}})

if [ -z "${CACHE_HIT}" ]; then
    echo "Cache miss for $@"
    exit 1
fi

exit 0
