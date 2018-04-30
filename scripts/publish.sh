#!/usr/bin/env bash

if [[ -v CIRRUS_TAG ]]; then
  gradle bintrayUpload
fi
