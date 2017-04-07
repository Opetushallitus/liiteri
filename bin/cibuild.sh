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
  clean-project
  process-resources
  echo "Creating uberjar"
  ./bin/lein with-profile uberjar uberjar
}

lint() {
  echo "Running Clojure Linter"
  ./bin/lein with-profile test-local eastwood
}

run-tests() {
  clean-project
  lint
  echo "Running tests"
  ./bin/lein test-local
}

COMMAND="$1"

case "$COMMAND" in
  "create-uberjar" )
    create-uberjar
    ;;
  "run-tests" )
    run-tests
    ;;
  *)
    echo "Unknown command: $COMMAND"
    ;;
esac
