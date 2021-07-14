import urllib3
import json
import logging
import os

logger = logging.getLogger()
logger.setLevel(logging.INFO)

http = urllib3.PoolManager()

HOOK_URL = os.environ["HOOK_URL"]
ENV=os.getenv("ENV", "-")


def lambda_handler(event, context):
    url = HOOK_URL

    message = json.loads(event['Records'][0]['Sns']['Message'])

    logger.info("Message: " + str(message))

    alarm_name = message['AlarmName']
    old_state = message['OldStateValue']
    new_state = message['NewStateValue']
    reason = message['NewStateReason']


    base_data = {
        "colour": "64a837",
        "title": "%s is resolved" %alarm_name,
        "text": "**%s** has changed from %s to %s - %s" %(alarm_name, old_state, new_state, reason)
    }
    if new_state.lower() == 'alarm':
        base_data = {
            "colour": "d63333",
            "title": "Red Alert - There is an issue %s" %alarm_name,
            "text": "**%s** has changed from %s to %s - %s" %(alarm_name, old_state, new_state, reason)
        }

    messages = {
        ('ALARM', 'my-alarm-name'): {
            "colour": "d63333",
            "title": "Red Alert - A bad thing happened.",
            "text": "These are the specific details of the bad thing."
        },
        ('OK', 'my-alarm-name'): {
            "colour": "64a837",
            "title": "The bad thing stopped happening",
            "text": "These are the specific details of how we know the bad thing stopped happening"
        }
    }
    data = messages.get((new_state, alarm_name), base_data)

    msg = {
        "@context": "https://schema.org/extensions",
        "@type": "MessageCard",
        "themeColor": data["colour"],
        "title": "[" + ENV + "] - " + data["title"],
        "text": data["text"] + "\n\n see details : \n\n" + str(message)
    }

    encoded_msg = json.dumps(msg).encode('utf-8')
    for i in range(3):
        try:
            resp = http.request('POST',url, body=encoded_msg)
            logger.info("POST message success")
            # print({
            #     "message": event['Records'][0]['Sns']['Message'],
            #     "status_code": resp.status,
            #     "response": resp.data
            # })
            return { "status": "200 OK"}
        except:
            logger.error("Request failed: " + str(i))

