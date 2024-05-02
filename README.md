# liiteri

[![Dependencies Status](https://jarkeeper.com/Opetushallitus/liiteri/status.svg)](https://jarkeeper.com/Opetushallitus/liiteri)

File Storage Service For OPH

## AWS S3

To use [AWS S3](https://aws.amazon.com/s3/) based storage engine, the service must have following configuration items:

```clojure
{:file-store {:engine :s3}
 ;; Other configuration items
 }
```

Also, when AWS S3 is used, AWS access key and AWS secret key must be provided to the service. Please see the
[Running The Service Locally](#running-the-service-locally) chapter for more info on how to provide the required
JVM system properties on startup.

## Testing

Tests require own separate database and Localstack. You can start them with Docker by hand

```bash
$ docker-compose up -d
```

and then run tests once by invoking

```bash
$ lein test-local
```

Run tests automatically on file changes by invoking

```bash
$ lein test-local-auto
```

You can also just run

```make test```

and it will take care of both Docker and running the tests.

Tests use the [dev-resources/test-config.edn](dev-resources/local-test-config.edn) configuration.

## Running The Service Locally

Quick start:

```make start```

When the REPL prompt opens, you can start the service by invoking

```clojure
(reset)
```

When you make changes to any files, run `(reset)` again.

Other REPL commands are as documented in the [reloaded.repl](https://github.com/weavejester/reloaded.repl) workflow.

The server runs on <http://localhost:16832/liiteri> by default. See <http://localhost:16832/liiteri/api-docs/index.html> for the Swagger API documentation.

In local setup, Localstack is used for S3. At startup /dev/resources/three_page_pdf_for_testing.pdf is loaded under the key
"4555c853-2a56-491f-b217-6e15a86aa0a8". You can load additional files and inspect the bucket with aws cli by
specifying the endpoint, e.g.:

```
export AWS_ACCESS_KEY_ID=localstack
export AWS_SECRET_ACCESS_KEY=localstack
aws s3 cp dev-resources/three_page_pdf_for_testing.pdf s3://opintopolku-untuva-liiteri/4555c853-2a56-491f-b217-6e15a86aa0a9 --endpoint-url http://localhost:4566
aws s3 ls s3://opintopolku-local-liiteri --endpoint-url http://localhost:4566
```

