import json
import boto3
import os
import logging
from aws_xray_sdk.core import xray_recorder
from aws_xray_sdk.core import patch_all

logger = logging.getLogger()
logger.setLevel(logging.INFO)
patch_all()

#region = "ap-southeast-1"
region = os.getenv('REGION')
ec2 = boto3.client("ec2", region_name=region)

### stop ec2 instances
def stop(instance_ids):
    try:
        logger.info ("stopped your instances: " + str(instance_ids))
        response = ec2.stop_instances(InstanceIds=[instance_ids])
        logger.info(response)
        return "stopped:OK"
    except:
        logger.error("An exception occurred:stop_instances")


### start ec2 instances
def start(instance_ids):
    try:
        logger.info ("started your instances: " + str(instance_ids))
        response = ec2.start_instances(InstanceIds=[instance_ids])
        logger.info(response)
        return "started:OK"
    except:
        logger.error("An exception occurred:start_instances")

@xray_recorder.capture("lambda_handler")
def lambda_handler(event, context):
    logger.info("Event :" + str(event))
    logger.info("context :" + str(context))
    for item in event['Records']:
        body=json.loads(item["body"])
        action=body["action"]
        for id in body["ids"]:
            if ('stop' == action):
                stop(id)
            elif (action == 'start'):
                start(id)

    return {
        'statusCode': 200,
        'body': 'ok'
    }

# {
#     "Records": [
#         {
#             "messageId": "2403aaf4-584b-447c-9562-e4271ef8f285",
#             "body": "{   \"action\": \"start\", \"ids\" : [ \"i-1f\",\"i-2\"]}",
#             "eventSource": "aws:sqs",
#             "eventSourceARN": "arn:aws:sqs:ap-southeast-1:329xxxx4857:sqs-test-queue",
#             "awsRegion": "ap-southeast-1"
#         },
#         {
#             "messageId": "2403aaf4-584b-447c-9562-e4271ef8f285",
#             "body": "{   \"action\": \"stop\", \"ids\" : [ \"i-1f\",\"i-2\"]}",
#             "eventSource": "aws:sqs",
#             "eventSourceARN": "arn:aws:sqs:ap-southeast-1:329xxxxx857:sqs-test-queue",
#             "awsRegion": "ap-southeast-1"
#         }
#     ]
# }