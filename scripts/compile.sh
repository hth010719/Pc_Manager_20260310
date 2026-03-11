#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$ROOT_DIR/out/classes"
mkdir -p "$OUT_DIR"
find "$ROOT_DIR/src/main/java" -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 -d "$OUT_DIR"
echo "Compiled to $OUT_DIR"
