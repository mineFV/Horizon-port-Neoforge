#!/usr/bin/env bash
# Builds the tool and the fixture jars, then checks what the tool says about them.
#   bash test/run-test.sh
# Needs a JDK 21 or newer. No real mod and no game file is used, and nothing is downloaded.
# The licence checks run Horizon-Translator's horizon-port.jar: HORIZON_PORT_JAR, or the one built in
# a Horizon-Translator clone beside this folder (bash mod-converter/build.sh there).
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bash build.sh
bash test/make-fixture.sh

B=test/build
F=test/fixture/pocketcompass
rm -rf "$B"
mkdir -p "$B/classes" "$B/stubs" "$B/mod" "$B/odd" "$B/jars" "$B/over" "$B/out" "$B/tmp"
OUT=$B/out/stdout.txt
ERR=$B/out/stderr.txt
failures=0

# The made-up mods, compiled against the stand-ins. Their jars hold their own classes and resources only.
javac --release 21 -d "$B/stubs" $(find test/fixture/stubs -name '*.java')
javac --release 21 -cp "$B/stubs" -d "$B/mod" $(find "$F/src" -name '*.java')
javac --release 21 -cp "$B/stubs" -d "$B/odd" $(find test/fixture/oddities/src -name '*.java')

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
  jar --create --file "$B/jars/$1.jar" --manifest "$F/MANIFEST.MF" -C "$B/mod" . -C "$dir" .
}

# lacks <file> <text>: the file doesn't hold the text.
lacks() {
  if grep -qF -- "$2" "$1"; then
    echo "  FAIL: '$2' is in $1"
    failures=$((failures + 1))
  else
    echo "  ok: no $2"
  fi
}

# plain_jar <name> <folder>: a jar of that folder alone.
plain_jar() { jar --create --file "$B/jars/$1.jar" -C "$2" .; }

# relicensed <name> <a `license = ...` line, or nothing>: $B/over/<name> holds the fixture's metadata
# with that line as its licence (with none, when the line is empty).
relicensed() {
  mkdir -p "$B/over/$1/META-INF"
  sed "s|^license = .*|$2|" "$F/resources/META-INF/neoforge.mods.toml" > "$B/over/$1/META-INF/neoforge.mods.toml"
}

# The jars most checks use: the two made-up mods, and the stand-ins passed off as NeoForge's own jar.
fixture_jar pocketcompass
J=$B/jars/pocketcompass.jar
jar --create --file "$B/jars/oddities.jar" -C "$B/odd" . -C test/fixture/oddities/resources .
jar --create --file "$B/jars/neoforge-stubs.jar" -C "$B/stubs" .

echo; echo "== unit tests"
javac --release 21 -Xlint:all -Werror -cp build/classes -d "$B/classes" test/java/horizonneo/*.java
for t in TomlTest JsonTest MetadataTest LicenceCheckTest ClassDataTest AnalyzerTest ReportTest StandInTest; do
  # Their arguments, for the tests that read them: the first mod's classes, its jar, the stand-ins' jar, the second mod's jar.
  java -cp "build/classes:$B/classes" "horizonneo.$t" "$B/mod" "$J" "$B/jars/neoforge-stubs.jar" "$B/jars/oddities.jar" || failures=$((failures + 1))
done

echo; echo "== metadata"
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
exits 1 tool metadata "$B/jars/no-such.jar"
says "$ERR" "isn't a file"

mkdir -p "$B/over/broken/META-INF"
printf 'license = "MIT"\n[[mods]]\nmodId = pocketcompass\n' > "$B/over/broken/META-INF/neoforge.mods.toml"
fixture_jar broken "$B/over/broken"
exits 1 tool metadata "$B/jars/broken.jar"
says "$ERR" 'META-INF/neoforge.mods.toml, line 3:'

echo; echo "== licence"
PORT_JAR=${HORIZON_PORT_JAR:-../Horizon-Translator/mod-converter/build/horizon-port.jar}
if [ ! -f "$PORT_JAR" ]; then
  echo "  FAIL: no horizon-port.jar at $PORT_JAR: build it in Horizon-Translator's clone (bash mod-converter/build.sh) or set HORIZON_PORT_JAR"
  failures=$((failures + 1))
else
  HORIZON_PORT_JAR="$(cd "$(dirname "$PORT_JAR")" && pwd)/$(basename "$PORT_JAR")"
  export HORIZON_PORT_JAR

  # An open licence: allowed, and the report names the mod's own metadata file.
  exits 0 tool license "$J"
  says "$OUT" '# Licence check: pocketcompass.jar'
  says "$OUT" '**Allowed**'
  says "$OUT" 'Declared in META-INF/neoforge.mods.toml: `MIT`'
  says "$OUT" '- Authors: Mine, Second Author'
  says "$OUT" "Checked by horizon-port's licence rules"
  lacks "$OUT" 'fabric.mod.json'
  relicensed gpl 'license = "GPL-3.0-or-later"'
  fixture_jar gpl "$B/over/gpl"
  exits 0 tool license "$B/jars/gpl.jar"
  says "$OUT" 'Conditions for the port'

  # A closed one: refused, unless one of the mod's authors says it is theirs.
  relicensed arr 'license = "All Rights Reserved"'
  fixture_jar arr "$B/over/arr"
  exits 3 tool license "$B/jars/arr.jar"
  says "$OUT" '**Not allowed**'
  says "$OUT" '(Mine, Second Author)'
  exits 3 tool license "$B/jars/arr.jar" --i-am-the-author Somebody
  exits 0 tool license "$B/jars/arr.jar" --i-am-the-author "second author"
  says "$OUT" '**Allowed**'
  exits 0 tool license "$B/jars/arr.jar" --i-am-the-author "Mine, Second Author"

  # No licence to go by: a placeholder nobody filled in, or no `license` at all.
  relicensed placeholder 'license = "${mod_license}"'
  fixture_jar placeholder "$B/over/placeholder"
  exits 3 tool license "$B/jars/placeholder.jar"
  says "$OUT" 'Declared in META-INF/neoforge.mods.toml: nothing'
  says "$OUT" 'build placeholder nobody filled in'
  relicensed missing ''
  fixture_jar missing "$B/over/missing"
  exits 3 tool license "$B/jars/missing.jar"
  says "$OUT" 'no `license`, which NeoForge requires'

  # The jar's licence files count too, and the strictest wins.
  relicensed closedfile 'license = "MIT"'
  echo "Copyright (c) 2026 Mine. All rights reserved." > "$B/over/closedfile/LICENSE"
  fixture_jar closedfile "$B/over/closedfile"
  exits 3 tool license "$B/jars/closedfile.jar"
  says "$OUT" 'Licence file LICENSE'
  relicensed openfile ''
  printf 'MIT License\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software.\n' > "$B/over/openfile/LICENSE.txt"
  fixture_jar openfile "$B/over/openfile"
  exits 0 tool license "$B/jars/openfile.jar"
  says "$OUT" 'Licence file LICENSE.txt'

  # A jar made for both loaders: both sets of metadata have to allow it.
  exits 2 tool license "$B/jars/both.jar"
  exits 0 tool license "$B/jars/both.jar" --loader neoforge
  says "$OUT" 'Both have to allow it: **allowed**'
  mkdir -p "$B/over/botharr"
  echo '{ "schemaVersion": 1, "id": "pocketcompass", "version": "1.2.3", "license": "ARR", "authors": ["Mine"] }' > "$B/over/botharr/fabric.mod.json"
  fixture_jar botharr "$B/over/botharr"
  exits 3 tool license "$B/jars/botharr.jar" --loader neoforge
  says "$OUT" 'Both have to allow it: **not allowed**'

  # A Fabric jar is horizon-port's: its answer, as it is.
  exits 0 tool license "$B/jars/fabric.jar"
  says "$OUT" 'Declared in fabric.mod.json: `MIT`'
  exits 0 tool license "$B/jars/both.jar" --loader fabric

  # Jars it doesn't read, and a horizon-port it can't find.
  exits 1 tool license "$B/jars/forge.jar"
  exits 1 tool license "$B/jars/broken.jar"
  exits 1 tool license "$J" --translator "$B/jars/no-such.jar"
  says "$ERR" "isn't a file (from --translator)"
  exits 1 env HORIZON_PORT_JAR="$B/nowhere.jar" java -jar build/horizon-port-neoforge.jar license "$J"
  says "$ERR" "isn't a file (from HORIZON_PORT_JAR)"
fi

echo; echo "== analyze"
exits 0 tool analyze "$J" --out "$B/report"
says "$OUT" 'Pocket Compass 1.2.3: 13 classes, 2 @Mod classes, 14 event listeners, 6 registrations, 2 Mixin classes'
says "$OUT" "Report: $B/report/report.md"
R=$B/report/report.json
says "$R" '"tool": "horizon-port-neoforge"'
says "$R" '"class": "com.example.pocketcompass.PocketCompassClient"'
says "$R" '"event": "net.neoforged.neoforge.client.event.ClientTickEvent$Post"'
says "$R" '"registeredBy": "addListener in PocketCompass'"'"'s constructor"'
says "$R" '"busFrom": "it is added to NeoForge.EVENT_BUS"'
says "$R" '"id": "pocketcompass:spinning_block"'
says "$R" '"class": "com.example.pocketcompass.SpinningBlock"'
says "$R" '"spec": "Config.SPEC"'
says "$R" '"annotation": "Redirect"'
says "$R" '"needs-mapping": 15'
says "$R" '"neoforgeJars": false'
M=$B/report/report.md
says "$M" '# Pocket Compass 1.2.3: what it does, and what converts to Horizon'
says "$M" 'Nothing was decompiled, and nothing of the mod was run.'
says "$M" '| `ClientTickEvent.Post` | not known | client | `ClientEvents.onClientTick` | @EventBusSubscriber on ClientEvents | needs-mapping |'
says "$M" '| `BlockEvent.BreakEvent` | game | common | `PocketCompass.onBreak` |'
says "$M" "The bus of 5 listeners isn't known."
says "$M" '| BLOCK | `pocketcompass:spinning_block` | `SpinningBlock` | `register` | `ModRegistry.SPINNING_BLOCK` | needs-translation |'
says "$M" '| `MinecraftMixin` | `Minecraft` | @Inject(HEAD) on tick, cancellable | `pocketcompass.mixins.json` (client) |'
says "$M" '- `public-f net.minecraft.client.Minecraft otherField`'
lacks "$M" '## Warnings'

# With NeoForge's own jars (here the stand-ins), each event's bus is read, not left unknown.
exits 0 tool analyze "$J" --out "$B/report-jars" --neoforge-jar "$B/jars/neoforge-stubs.jar"
says "$B/report-jars/report.md" '| `ClientTickEvent.Post` | game | client | `ClientEvents.onClientTick` |'
says "$B/report-jars/report.json" '"neoforgeJars": true'
lacks "$B/report-jars/report.md" "isn't known."

# Without --out, the report goes to out/<mod id> in the current folder.
mkdir -p "$B/cwd"
( cd "$B/cwd" && java -jar ../../../build/horizon-port-neoforge.jar analyze ../jars/pocketcompass.jar > /dev/null )
if [ -f "$B/cwd/out/pocketcompass/report.md" ] && [ -f "$B/cwd/out/pocketcompass/report.json" ]; then
  echo "  ok: the default folder is out/<mod id>"
else
  echo "  FAIL: no report in $B/cwd/out/pocketcompass"
  failures=$((failures + 1))
fi

# What can't be read is said in the report.
exits 0 tool analyze "$B/jars/oddities.jar" --out "$B/report-odd"
says "$B/report-odd/report.md" '## Warnings'
says "$B/report-odd/report.md" '- @Mod("somethingelse") on Oddities names no mod of META-INF/neoforge.mods.toml.'
says "$B/report-odd/report.md" "The handler isn't a lambda or a method reference written at the call, so it wasn't read."
says "$B/report-odd/report.md" 'in the jar, but the metadata doesn'"'"'t list it'

exits 1 tool analyze "$J" --out "$B/report-x" --neoforge-jar "$B/jars/no-such.jar"
says "$ERR" "--neoforge-jar: "
exits 1 tool analyze "$B/jars/forge.jar" --out "$B/report-x"
exits 1 tool analyze "$B/jars/broken.jar" --out "$B/report-x"
exits 2 tool analyze
if [ -f "${HORIZON_PORT_JAR:-}" ]; then
  # A Fabric jar is horizon-port's: its own report (it gives a verdict, this tool's doesn't), written where asked.
  exits 0 tool analyze "$B/jars/fabric.jar" --out "$B/report-fabric"
  says "$B/report-fabric/report.json" '"verdict":'
  lacks "$B/report-fabric/report.json" '"tool": "horizon-port-neoforge"'
fi

# The older way to name a Mixin config, in the jar's manifest: said, not acted on.
printf 'Implementation-Version: 1.2.3\nMixinConfigs: legacy.mixins.json, pocketcompass.mixins.json\n' > "$B/over/manifest.mf"
jar --create --file "$B/jars/manifestmixins.jar" --manifest "$B/over/manifest.mf" -C "$B/mod" . -C "$F/resources" .
exits 0 tool analyze "$B/jars/manifestmixins.jar" --out "$B/report-manifest"
says "$B/report-manifest/report.md" "the jar's manifest names the Mixin config legacy.mixins.json"
lacks "$B/report-manifest/report.md" "names the Mixin config pocketcompass.mixins.json"

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
