#!/usr/bin/env bash
opts=$@

echo ${opts}

validator_path="/app/bin/load-generator"

${validator_path} ${opts}



