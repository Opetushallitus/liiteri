#!/bin/bash

set -e

if [ $# -ne 6 ]
then
    printf "Usage: $0 <db host> <db port> <db name> <output dir> <username> <password>\n"
    exit 1
fi

HOST=$1
PORT=$2
DB=$3
OUT=$4
PREFIX="$4/liiteri"
USERNAME=$5
PASSWORD=$6

mkdir -p "${OUT}"
postgresql_autodoc -s public -d "${DB}" -h "${HOST}" -p "${PORT}" -f "${PREFIX}" -u "${USERNAME}" --password="${PASSWORD}" && dot -Tpng "${PREFIX}.dot" -o"${PREFIX}.png"
