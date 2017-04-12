#!/bin/bash

set -e

if [ $# -ne 7 ]
then
    printf "Usage: $0 <db host> <db port> <db name> <output dir> <version> <username> <password>\n"
    exit 1
fi

HOST=$1
PORT=$2
DB=$3
OUT=$4
VERSION=$5
PREFIX="$4/liiteri-${VERSION}"
USERNAME=$6
PASSWORD=$7

mkdir -p "${OUT}"
postgresql_autodoc -s public -d "${DB}" -h "${HOST}" -p "${PORT}" -f "${PREFIX}" -u "${USERNAME}" --password="${PASSWORD}" && dot -Tpng "${PREFIX}.dot" -o"${PREFIX}.png"
