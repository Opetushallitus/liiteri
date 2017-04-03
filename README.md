# liiteri

File Storage Service For OPH

## Filesystem Based Storage

To use filesystem based storage engine, the service must have following configuration items:

```clojure
{:file-store {:engine :filesystem
              :filesystem {:base-path "/path/to/storage"}}
 ;; Other configuration items
 }
```

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

Tests require own separate database. Open your terminal and run following command to start it:

```bash
$ docker run --name liiteri-test-db -e POSTGRES_PASSWORD=oph -e POSTGRES_USER=oph -e POSTGRES_DB=liiteri -p 5435:5432 -d postgres:9.5
```

Run tests once by invoking

```bash
$ lein test
```

Run tests automatically on file changes by invoking

```bash
$ lein test-auto
```

## Running The Service Locally

Open your terminal and run following commands. Creating the PostgreSQL Docker image needs to be done only once. If you
are running the service with filesystem based storage engine, the `JVM_OPTS` part of the startup command below can be
omitted.

```bash
$ docker run --name liiteri-dev-db -e POSTGRES_PASSWORD=oph -e POSTGRES_USER=oph -e POSTGRES_DB=liiteri -p 5434:5432 -d postgres:9.5
$ JVM_OPTS="-Daws.accessKeyId=access-key -Daws.secretKey=secret-key" lein repl
```

When the REPL prompt opens, you can start the service by invoking

```clojure
(reset)
```

When you make changes to any files, run `(reset)` again.

Other REPL commands are as documented in the [reloaded.repl](https://github.com/weavejester/reloaded.repl) workflow.

The server runs on <http://localhost:16832/liiteri> by default.
