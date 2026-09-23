#!/usr/bin/env bash
# Builds and runs the JO Mock API on http://localhost:8080 (see application.yml
# for the port). Runs in the foreground - stop it with Ctrl+C.
set -euo pipefail

cd "$(dirname "$0")"
./gradlew bootRun
