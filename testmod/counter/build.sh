#!/usr/bin/env bash
# Builds the made-up test mod Counter against a NeoForge server's jars (the one `dump` uses):
#   bash testmod/counter/build.sh <server folder> <out jar>
# Its source is ours (MIT); the jar it makes isn't committed (.gitignore refuses jars).
set -euo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
server="$1"; out="$2"
work="$(mktemp -d)"; trap 'rm -rf "$work"' EXIT
cp="$(find "$server/libraries" -name '*.jar' | grep -v '/net/minecraft/server/' | sort | tr '\n' ':')"
cp="$(find "$server/libraries" -name 'neoforge-*-server.jar' -o -name 'server-*-srg.jar' | sort -r | tr '\n' ':')$cp"
mkdir -p "$work/classes"
javac --release 21 -nowarn -cp "$cp" -d "$work/classes" $(find "$here/src" -name '*.java')
cp -r "$here/resources/." "$work/classes/"
jar --create --file "$out" -C "$work/classes" .
echo "Built $out"
