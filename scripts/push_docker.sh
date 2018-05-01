#!/usr/bin/env bash

set -e

docker login --username $DOCKER_USER_NAME --password $DOCKER_PASSWORD

docker push cirrusci/anka-controller-extended:${CIRRUS_TAG}
docker push cirrusci/anka-controller-extended:latest
