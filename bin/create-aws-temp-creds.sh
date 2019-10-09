#!/usr/bin/env bash

set -euo pipefail

CREDENTIALS_ANSWER=$(aws sts assume-role --profile=oph-dev --role-arn=arn:aws:iam::153563371259:role/CustomerCloudAdmin --role-session-name=LocalLiiteriDvaus)

AccessKeyId=$(echo $CREDENTIALS_ANSWER | jq -r '.Credentials.AccessKeyId')
SecretAccessKey=$(echo $CREDENTIALS_ANSWER | jq -r '.Credentials.SecretAccessKey')
SessionToken=$(echo $CREDENTIALS_ANSWER | jq -r '.Credentials.SessionToken')
Expiration=$(echo $CREDENTIALS_ANSWER | jq -r '.Credentials.Expiration')

echo
echo "Got $CREDENTIALS_ANSWER"
echo
echo "Add the following to your environment before starting Liiteri. It will be valid until $Expiration"
echo
echo "JVM_OPTS=\"-Daws.accessKeyId=$AccessKeyId -Daws.secretKey=$SecretAccessKey -Daws.sessionToken=$SessionToken\""
