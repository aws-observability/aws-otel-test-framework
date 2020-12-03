#!/usr/bin/env bash
opts=$@

echo ${opts}

validator_path="/app/bin/app"

${validator_path} ${opts}



