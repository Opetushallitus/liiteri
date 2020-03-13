#!/bin/bash

set -euo pipefail

CREDENTIALS_ANSWER=$(aws sts assume-role --profile=oph-dev --role-arn=arn:aws:iam::153563371259:role/CustomerCloudAdmin --role-session-name=LocalLiiteriDvaus)

AccessKeyId=$(echo $CREDENTIALS_ANSWER | jq -r '.Credentials.AccessKeyId')
SecretAccessKey=$(echo $CREDENTIALS_ANSWER | jq -r '.Credentials.SecretAccessKey')
SessionToken=$(echo $CREDENTIALS_ANSWER | jq -r '.Credentials.SessionToken')
Expiration=$(echo $CREDENTIALS_ANSWER | jq -r '.Credentials.Expiration')
currentDir=`pwd`
repositoryRoot="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." >/dev/null 2>&1 && pwd )"

cd ${repositoryRoot}

JVM_OPTS="-Daws.accessKeyId=$AccessKeyId -Daws.secretKey=$SecretAccessKey -Daws.sessionToken=$SessionToken\"" \
  lein repl

cd ${currentDir}
