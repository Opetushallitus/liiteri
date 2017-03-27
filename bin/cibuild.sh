#!/bin/bash
set -e

OLD_CWD=$(pwd)

export JAVA_HOME=/data00/oph/java/jdk1.8.0_60
export PATH=/data00/oph/java/jdk1.8.0_60/bin:$PATH

clean-project() {
  echo "Cleaning project"
  ./bin/lein clean
}

create-uberjar() {
  clean-project
  echo "Creating uberjar"
  ./bin/lein with-profile uberjar uberjar
}

run-tests() {
  clean-project
  echo "Running tests"
  ./bin/lein test
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
