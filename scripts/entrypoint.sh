#!/usr/bin/env bash

set -e

if [[ -v ANYCONNECT_SERVER ]]; then
  ip tuntap add vpn0 mode tun0

  ( echo yes; echo $ANYCONNECT_PASSWORD ) | \
    openconnect $ANYCONNECT_SERVER --user=$ANYCONNECT_USER --timestamp --background
fi

exec java -jar anka-controller-extended-all.jar server /svc/anka-controller-extended/anka_controller_extended_config.yml