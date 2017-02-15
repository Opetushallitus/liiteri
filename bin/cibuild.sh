#!/bin/bash
set -e

OLD_CWD=$(pwd)

create-uberjar() {
  echo "Cleaning project"
  ./bin/lein clean
  echo "Creating uberjar"
  ./bin/lein with-profile uberjar uberjar
}

COMMAND="$1"

case "$COMMAND" in
  "create-uberjar" )
    create-uberjar
    ;;
  *)
    echo "Unknown command: $COMMAND"
    ;;
esac
