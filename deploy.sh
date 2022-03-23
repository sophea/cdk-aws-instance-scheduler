#!/bin/bash
set -eo pipefail

if [ $# -eq 1 ]; then
    echo "$0 service with profile $1 "
else
    echo "Failed action $# . The usage is $0 dev|sit|prod|shared"
    exit 1;
fi

env="$1"

mvn clean package

MODULE="java-schedule-ec2-lambda"
cp $MODULE/target/ec2-lambda-java.jar assets/ec2-lambda-java.jar

echo "==========================================================="
echo "***********DEPLOYING INTO ${env^^} ENVIRONMENT *************"
echo "==========================================================="
cd cdk
cdk deploy -c ENV=${env} --profile ${env} --require-approval never
cd ..
