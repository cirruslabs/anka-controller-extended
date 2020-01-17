#!/usr/bin/env bash

if ! pgrep -x "openconnect" > /dev/null
then
  if [[ -v ANYCONNECT_SERVER ]]; then
    ( echo yes; echo $ANYCONNECT_PASSWORD ) | \
      openconnect $ANYCONNECT_SERVER --user=$ANYCONNECT_USER --timestamp --background
    sleep 5
  fi
fi

curl --fail http://localhost:8081/healthcheck || exit 1
