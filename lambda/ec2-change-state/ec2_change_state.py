import json
import logging
import os
import boto3
from urllib.request import Request, urlopen
from urllib.error import URLError, HTTPError
from aws_xray_sdk.core import xray_recorder
from aws_xray_sdk.core import patch_all

HOOK_URL = os.environ["HookUrl"]
ENV=os.getenv("ENV", "-")

logger = logging.getLogger()
logger.setLevel(logging.INFO)
patch_all()


#Event:
#{"instance-id": "i-093fxxxxx72017bc9d",
#"state": "stopping",
#"time": "2021-04-13T08:42:33Z",
#"region": "ap-southeast-1",
#"account": "6743xxxx895",
# "message": "At 2021-04-13T08:42:33Z, the status of your EC2 instance i-09xxx17bc9d on account 6xxxxx895 in the AWS Region ap-southeast-1 has changed to stopping."
# }


def get_name(instance):
    for t in instance["Tags"]:
        if t["Key"] == "Name":
            return t["Value"]
    return instance["InstanceId"]

def get_instance_details(client, instanceId):
    instances = []
    data = client.describe_instances(InstanceIds=[instanceId])
    for res in data["Reservations"]:
        instances.extend(res["Instances"])

    return instances

@xray_recorder.capture("lambda_handler")
def lambda_handler(event, context):
    logger.info("Event: " + str(event))
    instanceId = event["instance-id"]
    state = event["state"]
    region = event["region"]
    account= event["account"]
    message = event["message"]


    ec2 = boto3.client('ec2',region_name=region)
    ##instances with tag and running state
    #instances = ec2.instances.filter(InstanceIds=[instanceId])
    instances = get_instance_details(ec2, instanceId)
    instanceName="NA"
    for i in instances:
        instanceName = get_name(i)
        logger.info("instance name :" + instanceName)

    message = message.replace(instanceId, "name " + instanceName + " [" + instanceId + "] ")
    logger.info("message" + message)


    messages = {
        "@context": "https://schema.org/extensions",
        "@type": "MessageCard",
        "themeColor": "64a837",
        "title": "[" + ENV +"] server " + instanceName + " has changed state to " + state,
        "text": message
    }


    req = Request(HOOK_URL, json.dumps(messages).encode("utf-8"))
    for i in range(3):
        try:
            response = urlopen(req)
            response.read()
            logger.info("Message posted to team")
            return { "status": "200 OK"}
        except HTTPError as e:
            logger.error("Request failed: %d %s", e.code, e.reason)
        except URLError as e:
            logger.error("Server connection failed: %s", e.reason)


