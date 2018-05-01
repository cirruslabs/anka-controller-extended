#!/usr/bin/env bash

if [[ -v CIRRUS_TAG ]]; then
  gradle :sdk:bintrayUpload --stacktrace --info
  gradle :client:bintrayUpload --stacktrace --info
fi
