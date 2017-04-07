# liiteri

[![CircleCI](https://circleci.com/gh/Opetushallitus/liiteri/tree/master.svg?style=shield&circle-token=bbd5744d1e6d1af5468a51b78bddb91e3deda767)](https://circleci.com/gh/Opetushallitus/liiteri/tree/master)
[![Dependencies Status](https://jarkeeper.com/Opetushallitus/liiteri/status.svg)](https://jarkeeper.com/Opetushallitus/liiteri)

File Storage Service For OPH

## Testing

Tests require own separate database. Open your terminal and run following command to start it:

```bash
$ docker run --name liiteri-test-db -e POSTGRES_PASSWORD=oph -e POSTGRES_USER=oph -e POSTGRES_DB=liiteri -p 5435:5432 -d postgres:9.5
```

Run tests once by invoking

```bash
$ lein test-local
```

Run tests automatically on file changes by invoking

```bash
$ lein test-local-auto
```

Tests use the [dev-resources/test-config.edn](dev-resources/local-test-config.edn) configuration.

## Running The Service Locally

Open your terminal and run following commands. Creating the PostgreSQL Docker image needs to be done only once. If you
are running the service with filesystem based storage engine, the `JVM_OPTS` part of the startup command below can be
omitted.

```bash
$ docker run --name liiteri-dev-db -e POSTGRES_PASSWORD=oph -e POSTGRES_USER=oph -e POSTGRES_DB=liiteri -p 5434:5432 -d postgres:9.5
$ lein repl
```

When the REPL prompt opens, you can start the service by invoking

```clojure
(reset)
```

When you make changes to any files, run `(reset)` again.

Other REPL commands are as documented in the [reloaded.repl](https://github.com/weavejester/reloaded.repl) workflow.

The server runs on <http://localhost:16832/liiteri> by default. See <http://localhost:16832/liiteri/api-docs/index.html> for the Swagger API documentation. It is generated from the API specified in [src/liiteri/api.clj](src/liiteri/api.clj#L28).
