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
# The translator's model of Java's library (lib/): our own java.lang and java.util, compiled in their place; and its
# stand-ins for the game (game/), compiled against it.
rm -rf "$out/model"
mkdir -p "$out/model" "$out/classes/model"
javac --patch-module java.base=lib -d "$out/model" $(find lib game -name '*.java' | sort)
(cd "$out/model" && find . -name '*.class' | sed 's|^\./||' | sort) > "$out/model/index.txt"
# Unicode's tables for the model's Character and String, from this JDK (data of its Unicode version, no code).
java --add-opens java.base/java.lang=ALL-UNNAMED tools/UnicodeTables.java "$out/classes/horizonneo/unicode.rs"
cp -r "$out/model/." "$out/classes/model/"
# The machines exporter isn't compiled here (it needs NeoForge's jars): `dump` compiles it against a server's own.
mkdir -p "$out/classes/exporter"
cp -r exporter/src/. exporter/resources/. "$out/classes/exporter/"
jar --create --file "$out/horizon-port-neoforge.jar" --main-class horizonneo.Main -C "$out/classes" .
echo "Built $out/horizon-port-neoforge.jar"
