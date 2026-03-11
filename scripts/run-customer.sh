#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
"$ROOT_DIR/scripts/compile.sh"
exec java -cp "$ROOT_DIR/out/classes" com.pcmanager.CustomerMain
