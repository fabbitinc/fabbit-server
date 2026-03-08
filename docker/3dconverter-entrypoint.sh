#!/bin/sh
set -eu

if [ "$#" -lt 1 ]; then
  echo "usage: 3dconverter-entrypoint <converter-args...>" >&2
  exit 2
fi

exec xvfb-run --auto-servernum /opt/mayo/MayoConv.AppImage \
  --appimage-extract-and-run \
  "$@"
