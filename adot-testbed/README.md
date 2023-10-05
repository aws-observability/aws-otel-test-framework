# Introduction

The tests require that AWS credentials are sent to the container under test. Please follow this procedure.


```
export AWS_REGION=us-west-2
export AWS_ACCESS_KEY_ID=<key>
export AWS_SECRET_ACCESS_KEY=<secret>
export AWS_SESSION_TOKEN=<session>
./gradlew test --rerun-tasks --info
```
