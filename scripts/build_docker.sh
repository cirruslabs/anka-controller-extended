#!/usr/bin/env bash

set -e

docker build --tag cirrusci/anka-controller-extended:${CIRRUS_TAG} --tag cirrusci/anka-controller-extended:latest .
