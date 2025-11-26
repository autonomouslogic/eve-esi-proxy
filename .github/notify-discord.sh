#!/bin/bash -e

URL="$1"
MSG="$2"

curl -m 10 --retry 5 -H "Content-Type: application/json" -d "{\"content\": \"$MSG\"}" $URL
