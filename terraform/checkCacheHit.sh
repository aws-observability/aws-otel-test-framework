#!/bin/bash

# Checks for cache hits for specific test case
# exits 1 if cache hit was missing
# required env vars
# DDB_TABLE_NAME: dyanmodb table name that will be queries
# TF_VAR_aoc_version: test image version that was used

test_framework_shortsha=$(git rev-parse --short HEAD)

CACHE_HIT=$(aws dynamodb get-item --region=us-west-2 --table-name ${DDB_TABLE_NAME} --key {\"TestId\":{\"S\":\"$1$2$3\"}\,\"aoc_version\":{\"S\":\"${TF_VAR_aoc_version}$test_framework_shortsha\"}})

if [ -z "${CACHE_HIT}" ]; then
    echo "Cache miss for $@"
    exit 1
fi

exit 0
