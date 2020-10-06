#!/bin/sh
set -e

module=${1}
opts=${2}

/app/validator/bin/validator ${opts}

