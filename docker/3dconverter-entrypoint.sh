#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
  echo "usage: 3dconverter-entrypoint <input-path> <output-path>" >&2
  exit 2
fi

INPUT_PATH="$1"
OUTPUT_PATH="$2"

exec xvfb-run --auto-servernum /opt/mayo/MayoConv.AppImage \
  --appimage-extract-and-run \
  --input "$INPUT_PATH" \
  --output "$OUTPUT_PATH"
