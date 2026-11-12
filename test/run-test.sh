#!/usr/bin/env bash
# Builds the tool and the fixture jars, then checks what the tool says about them.
#   bash test/run-test.sh
# Needs a JDK 21 or newer. No real mod and no game file is used, and nothing is downloaded.
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bash build.sh
bash test/make-fixture.sh

B=test/build
F=test/fixture/pocketcompass
rm -rf "$B"
mkdir -p "$B/classes" "$B/jars" "$B/over" "$B/out" "$B/tmp"
OUT=$B/out/stdout.txt
ERR=$B/out/stderr.txt
failures=0

# The tool, with its temporary files in the build folder, so the tests can see that none is left.
tool() { java -Djava.io.tmpdir="$PWD/$B/tmp" -jar build/horizon-port-neoforge.jar "$@"; }

# exits <code> <command...>: the command ends with that code. What it prints goes to $OUT and $ERR.
exits() {
  local want=$1 got=0
  shift
  "$@" > "$OUT" 2> "$ERR" || got=$?
  if [ "$got" = "$want" ]; then
    echo "  ok: exit $want: $*"
  else
    echo "  FAIL: exit $got instead of $want: $*"
    sed 's/^/        /' "$ERR" | head -5
    failures=$((failures + 1))
  fi
}

# says <file> <text>: the file holds the text.
says() {
  if grep -qF -- "$2" "$1"; then
    echo "  ok: $2"
  else
    echo "  FAIL: '$2' isn't in $1"
    failures=$((failures + 1))
  fi
}

# fixture_jar <name> [<folder laid over the fixture's resources>]: $B/jars/<name>.jar
fixture_jar() {
  local dir="$B/jars/$1.d"
  mkdir -p "$dir"
  cp -r "$F/resources/." "$dir/"
  if [ $# -ge 2 ]; then cp -r "$2/." "$dir/"; fi
  jar --create --file "$B/jars/$1.jar" --manifest "$F/MANIFEST.MF" -C "$dir" .
}

# plain_jar <name> <folder>: a jar of that folder alone.
plain_jar() { jar --create --file "$B/jars/$1.jar" -C "$2" .; }

echo; echo "== unit tests"
javac --release 21 -Xlint:all -Werror -cp build/classes -d "$B/classes" test/java/horizonneo/*.java
for t in TomlTest MetadataTest; do
  java -cp "build/classes:$B/classes" "horizonneo.$t" || failures=$((failures + 1))
done

echo; echo "== metadata"
fixture_jar pocketcompass
J=$B/jars/pocketcompass.jar
exits 0 tool metadata "$J"
says "$OUT" 'Pocket Compass 1.2.3 (`pocketcompass`), a NeoForge mod: pocketcompass.jar'
says "$OUT" 'licence: MIT'
says "$OUT" 'licence files: none'
says "$OUT" 'authors: Mine, Second Author'
says "$OUT" "description: Shows which way you face on the HUD. Made up for the converter's tests."
says "$OUT" 'Mixin configs: pocketcompass.mixins.json'
says "$OUT" 'access transformers: META-INF/accesstransformer.cfg'
says "$OUT" 'depends on: neoforge (required, [26.3,))'
says "$OUT" 'depends on: jei (optional, *, client only)'
exits 0 tool metadata "$J" --json
says "$OUT" '"id": "pocketcompass"'
says "$OUT" '"version": "1.2.3"'
says "$OUT" '"license": "MIT"'
says "$OUT" '"versionRange": "[26.3,27)"'
says "$OUT" '"warnings": []'

echo; echo "== jars it doesn't read"
mkdir -p "$B/over/forge/META-INF"
cp "$F/resources/META-INF/neoforge.mods.toml" "$B/over/forge/META-INF/mods.toml"
plain_jar forge "$B/over/forge"
exits 1 tool metadata "$B/jars/forge.jar"
says "$ERR" "is Forge's, or NeoForge's before 20.5"

mkdir -p "$B/over/fabric"
echo '{ "schemaVersion": 1, "id": "pocketcompass", "version": "1.2.3", "license": "MIT", "authors": ["Mine"] }' > "$B/over/fabric/fabric.mod.json"
plain_jar fabric "$B/over/fabric"
exits 1 tool metadata "$B/jars/fabric.jar"
says "$ERR" 'is a Fabric mod'

fixture_jar both "$B/over/fabric"
exits 2 tool metadata "$B/jars/both.jar"
says "$ERR" 'made for both NeoForge and Fabric'
exits 0 tool metadata "$B/jars/both.jar" --loader neoforge
says "$OUT" 'also: a fabric.mod.json'

mkdir -p "$B/over/empty"
echo "hello" > "$B/over/empty/readme.txt"
plain_jar empty "$B/over/empty"
exits 1 tool metadata "$B/jars/empty.jar"
says "$ERR" "isn't a NeoForge or Fabric mod"

echo "not a zip" > "$B/jars/text.jar"
exits 1 tool metadata "$B/jars/text.jar"
says "$ERR" "isn't a jar"
exits 1 tool metadata "$B/jars/missing.jar"
says "$ERR" "isn't a file"

mkdir -p "$B/over/broken/META-INF"
printf 'license = "MIT"\n[[mods]]\nmodId = pocketcompass\n' > "$B/over/broken/META-INF/neoforge.mods.toml"
fixture_jar broken "$B/over/broken"
exits 1 tool metadata "$B/jars/broken.jar"
says "$ERR" 'META-INF/neoforge.mods.toml, line 3:'

echo; echo "== the command line"
exits 0 tool --help
exits 2 tool
exits 2 tool metadata
exits 2 tool metadata "$J" --nope
exits 2 tool metadata "$J" --loader quilt
exits 2 tool nope

echo
left=$(find "$B/tmp" -mindepth 1 | wc -l)
if [ "$left" = 0 ]; then
  echo "  ok: no temporary file left behind"
else
  echo "  FAIL: $left temporary files left in $B/tmp"
  failures=$((failures + 1))
fi

echo
if [ "$failures" = 0 ]; then
  echo "Everything passed."
else
  echo "$failures checks failed."
  exit 1
fi
