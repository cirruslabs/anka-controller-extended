#!/usr/bin/env bash

if [[ -v CIRRUS_TAG ]]; then
  ./gradlew :sdk:bintrayUpload --stacktrace --info
  ./gradlew :client:bintrayUpload --stacktrace --info
fi
