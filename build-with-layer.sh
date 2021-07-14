#!/bin/bash
set -eo pipefail

MODULE="java-schedule-ec2-lambda"
mvn -f $MODULE/pom.xml install
cp $MODULE/target/ec2-lambda-java-lib-mvn.zip assets/ec2-lambda-java-lib-mvn.zip