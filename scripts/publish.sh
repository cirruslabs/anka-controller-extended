#!/usr/bin/env bash

if [[ -v CIRRUS_TAG ]]; then
  gradle :client:bintrayUpload --stacktrace --info
fi
