#!/bin/bash
set -e

# Please note that Circle CI uses circle.yml instead of cibuild.sh

OLD_CWD=$(pwd)

export JAVA_HOME=/data00/oph/java/jdk1.8.0_60
export PATH=/data00/oph/java/jdk1.8.0_60/bin:$PATH

clean-project() {
  echo "Cleaning project"
  ./bin/lein clean
}

process-resources() {
  echo "Processing resources"
  ./bin/lein resource
}

create-uberjar() {
  process-resources
  echo "Creating uberjar"
  ./bin/lein with-profile uberjar uberjar
}

run-tests() {
  echo "Running tests"
  ./bin/lein test-ci
}

create-db-schema() {
  echo "Creating DB schema diagrams"
  ./bin/lein db-schema
}

COMMAND="$1"

case "$COMMAND" in
  "create-uberjar" )
    clean-project
    create-uberjar
    ;;
  "run-tests" )
    clean-project
    run-tests
    ;;
  "run-tests-and-create-uberjar" )
    clean-project
    run-tests
    create-uberjar
    ;;
  "create-db-schema" )
    clean-project
    create-db-schema
    ;;
  *)
    echo "Unknown command: $COMMAND"
    ;;
esac
