#!/usr/bin/env bash
# Builds build/horizon-port-neoforge.jar from src/. Needs a JDK 21 or newer (javac and jar), nothing else.
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

for tool in javac jar; do
  command -v "$tool" > /dev/null || { echo "build.sh: $tool not found (install a JDK 21 or newer)" >&2; exit 1; }
done

out=build
rm -rf "$out/classes"
mkdir -p "$out/classes"
find src -name '*.java' | sort > "$out/sources.txt"
# Warnings are errors: the tool has no dependencies to blame them on.
javac --release 21 -Xlint:all -Werror -d "$out/classes" @"$out/sources.txt"
jar --create --file "$out/horizon-port-neoforge.jar" --main-class horizonneo.Main -C "$out/classes" .
echo "Built $out/horizon-port-neoforge.jar"
