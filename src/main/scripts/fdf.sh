#!/usr/bin/env bash

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
java -jar "$APP_DIR/find-dup-files.jar" "$@"
