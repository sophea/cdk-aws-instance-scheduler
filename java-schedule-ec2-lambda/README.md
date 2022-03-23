# Blank function (Java)

![Architecture](/sample-apps/blank-java/images/sample-blank-java.png)

The project source includes function code and supporting resources:

- `src/main` - A Java function.
- `src/test` - A unit test and helper classes.
- `template.yml` - An AWS CloudFormation template that creates an application.
- `build.gradle` - A Gradle build file.
- `pom.xml` - A Maven build file.
- `1-create-bucket.sh`, `2-build-layer.sh`, etc. - Shell scripts that use the AWS CLI to deploy and manage the application.

Use the following instructions to deploy the sample application.

# Requirements
- [Java 8 runtime environment (SE JRE)](https://www.oracle.com/java/technologies/javase-downloads.html)
- [Gradle 5](https://gradle.org/releases/) or [Maven 3](https://maven.apache.org/docs/history.html)
- The Bash shell. For Linux and macOS, this is included by default. In Windows 10, you can install the [Windows Subsystem for Linux](https://docs.microsoft.com/en-us/windows/wsl/install-win10) to get a Windows-integrated version of Ubuntu and Bash.
- [The AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) v1.17 or newer.

If you use the AWS CLI v2, add the following to your [configuration file](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html) (`~/.aws/config`):

```
cli_binary_format=raw-in-base64-out
```

This setting enables the AWS CLI v2 to load JSON events from a file, matching the v1 behavior.

# Setup
Download or clone this repository.

    $ git clone https://github.com/awsdocs/aws-lambda-developer-guide.git
    $ cd aws-lambda-developer-guide/sample-apps/blank-java

To create a new bucket for deployment artifacts, run `1-create-bucket.sh`.

    blank-java$ ./1-create-bucket.sh
    make_bucket: lambda-artifacts-a5e491dbb5b22e0d

To build a Lambda layer that contains the function's runtime dependencies, run `2-build-layer.sh`. Packaging dependencies in a layer reduces the size of the deployment package that you upload when you modify your code.

    blank-java$ ./2-build-layer.sh
    
    blank-java$ ./2-build-layer-mvn.sh  (maven build)

# Deploy

To deploy the application, run `3-deploy.sh`.

    blank-java$ ./3-deploy.sh
    BUILD SUCCESSFUL in 1s
    Successfully packaged artifacts and wrote output template to file out.yml.
    Waiting for changeset to be created..
    Successfully created/updated stack - blank-java

This script uses AWS CloudFormation to deploy the Lambda functions and an IAM role. If the AWS CloudFormation stack that contains the resources already exists, the script updates it with any changes to the template or function code.

You can also build the application with Maven. To use maven, add `mvn` to the command.

    java-basic$ ./3-deploy.sh mvn
    [INFO] Scanning for projects...
    [INFO] -----------------------< com.example:blank-java >-----------------------
    [INFO] Building blank-java-function 1.0-SNAPSHOT
    [INFO] --------------------------------[ jar ]---------------------------------
    ...

# Test
To invoke the function, run `4-invoke.sh`.

    blank-java$ ./4-invoke.sh
    {
        "StatusCode": 200,
        "ExecutedVersion": "$LATEST"
    }

Let the script invoke the function a few times and then press `CRTL+C` to exit.

The application uses AWS X-Ray to trace requests. Open the [X-Ray console](https://console.aws.amazon.com/xray/home#/service-map) to view the service map.

![Service Map](/sample-apps/blank-java/images/blank-java-servicemap.png)

Choose a node in the main function graph. Then choose **View traces** to see a list of traces. Choose any trace to view a timeline that breaks down the work done by the function.

![Trace](/sample-apps/blank-java/images/blank-java-trace.png)

Finally, view the application in the Lambda console.

*To view the application*
1. Open the [applications page](https://console.aws.amazon.com/lambda/home#/applications) in the Lambda console.
2. Choose **blank-java**.

  ![Application](/sample-apps/blank-java/images/blank-java-application.png)

# Cleanup
To delete the application, run `5-cleanup.sh`.

    blank$ ./5-cleanup.sh

# json sample

````
[
  {
    "id":"1",
    "days": [
      "mon",
      "tue",
      "wed",
      "thu",
      "fri",
      "sat"
    ],
    "stopTime": "18:00",
    "startTime": "09:50",
    "instanceIds": [
      "id1",
      "id2"
    ]
  },
  {
    "id":"2",
    "allDays": true,
    "stopTime": "18:00",
    "startTime1": "08:00",
    "instanceIds": [
      "i-00a3fde469d58dd16",
      "i-03b67a5e87e367846"
    ]
  }
]


=============SIT=========
[
  {
    "days": [
      "mon",
      "tue",
      "wed",
      "thu",
      "fri"
    ],
    "stopTime": "18:00",
    "instanceIds": [
      "i-04ac3b83af9972ccc",
      "i-079e49b8f04529acc", 
      "i-0aa4bca255ad7f4e6",
      "i-0cf3f4af1a1488fd7", 
      "i-093f3ea672017bc9d",
      "i-0a58ce71f23cb22ec",
      "i-0ebf5feba74d3e678",
      "i-07fc87d755dc0dcdb"
    ]
  },

  {
    "allDays": true,
    "stopTime": "22:00",
    "instanceIds": [
      "i-07fa58acb5f6f1046 ",
      "i-0f4fed2e1c285d044"
    ]
  }

{
    "allDays": false,
    "days": [
          "mon",
          "tue",
          "wed",
          "thu",
          "fri"
        ],
    "stopTime": "23:00",
    "startTime" : "08:00",
    "instanceIds": [
      "i-07fa58acb5f6f1046 ",
      "i-0f4fed2e1c285d044"
    ]
  }
]


````

###Lambda function environment
````
SCHEDULE_VALUE_TEST	[{"days":["mon","tue","wed","thu","fri","sat"],"stopTime":"18:00","startTime":"09:50","instanceIds":["id1","id2"]},{"allDays":true,"stopTime":"11:55","startTime":"14:52","instanceIds":["id12","id22"]}]

TEST json {"TEST":1}
````
