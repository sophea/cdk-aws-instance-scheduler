#!/bin/bash
set -eo pipefail

env="shared"

mvn package

MODULE="java-schedule-ec2-lambda"
cp $MODULE/target/ec2-lambda-java.jar assets/ec2-lambda-java.jar

echo "==========================================================="
echo "***********DEPLOYING INTO ${env^^} ENVIRONMENT *************"
echo "==========================================================="
cd cdk
cdk deploy -c ENV=${env} --profile ${env} --require-approval never
cd ..