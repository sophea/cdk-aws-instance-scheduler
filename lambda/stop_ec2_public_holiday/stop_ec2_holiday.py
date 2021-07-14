from datetime import datetime

import json
import boto3
import os

today=datetime.today().strftime('%Y-%m-%d')

ec2 = boto3.client('ec2')

region = os.getenv('REGION')

public_holidays= os.getenv('PUBLIC_HOLIDAYS')
excluded_ids = os.getenv('EXCLUDED_IDS')

#ec2 = boto3.client('ec2',region_name=region)
ec2_resource = boto3.resource('ec2',region_name=region)

def lambda_handler(event, context):

    isHoliday = False

    print("today :" + today)
    print("event " + str(event))
    # print("context " + str(context))
    print("public_holidays list " + public_holidays)
    print("excluded_ids list " + excluded_ids)

    days_json  = json.loads(public_holidays)
    days = days_json["days"]

    #days = event["days"]
    #excludes = event["excludes"]
    excludes = json.loads(excluded_ids)["excludes"]
    if today in days :
        print ("found - it is public holiday : " + today)
        isHoliday = True

    if (not isHoliday):
        print ("It is not public holiday " + today)
        return {'body' : "nothing to process"}
    ## stop all running instances

    instances = ec2_resource.instances.filter(Filters=[{'Name': 'instance-state-name', 'Values': ['running']}])
    running_instances = [instance.id  for instance in instances if instance.id not in excludes]
    for id in running_instances:
        #ec2.stop_instances(InstanceIds=[id])
        ec2_resource.Instance(id).stop()
        print ("stop_instances : " + id)

    return {
        'statusCode': 200,
        'body': json.dumps('success from Lambda!')
    }
