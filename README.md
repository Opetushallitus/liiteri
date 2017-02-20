# liiteri

File Storage Service For OPH

## AWS S3

The backing object storage is [AWS S3](https://aws.amazon.com/s3/). This service assumes that versioning support is turned on for the bucket used.

## Running The Service Locally

Open your terminal and run following commands. Creating the PostgreSQL Docker image needs to be done only once.

```bash
$ docker run --name liiteri-dev-db -e POSTGRES_PASSWORD=oph -e POSTGRES_USER=oph -e POSTGRES_DB=liiteri -p 5434:5432 -d postgres:9.5
$ JVM_OPTS="-Daws.accessKeyId=access-key -Daws.secretKey=secret-key" lein repl
```

When the REPL prompt opens, you can start the service by invoking

```clojure
(reset)
```

Other REPL commands are as documented in the [reloaded.repl](https://github.com/weavejester/reloaded.repl) workflow.
