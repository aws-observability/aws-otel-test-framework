#!/usr/bin/env bash

running_type=${1}
opts=${2}

echo ${opts}

validator_path="/app/validator/bin/validator"

#execute
execute()
{
  case ${running_type} in
    validator)
      ${validator_path} "${opts}"
      ;;
  esac
}

execute


