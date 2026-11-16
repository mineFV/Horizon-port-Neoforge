#!/usr/bin/env bash
# Builds the tool and the fixture jars, then checks what the tool says about them.
#   bash test/run-test.sh
# Needs a JDK 21 or newer. No real mod and no game file is used, and nothing is downloaded.
# The licence checks run Horizon-Translator's horizon-port.jar: HORIZON_PORT_JAR, or the one built in
# a Horizon-Translator clone beside this folder (bash mod-converter/build.sh there).
# The checks that pack a .hmod run Horizon's own content tools: HORIZON_TOOLS, or the ones cargo built in a
# Horizon clone beside this folder. They are skipped, with a note, when the tools aren't there.
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bash build.sh
bash test/make-fixture.sh

B=test/build
F=test/fixture/pocketcompass
rm -rf "$B"
mkdir -p "$B/classes" "$B/stubs" "$B/mod" "$B/odd" "$B/tidy" "$B/stray" "$B/screen" "$B/wrapped" "$B/jars" "$B/over" "$B/out" "$B/tmp"
OUT=$B/out/stdout.txt
ERR=$B/out/stderr.txt
failures=0
# Horizon's own content tools, built in its source with cargo: only the checks that pack a .hmod use them, and
# they are skipped when the tools aren't there. Every other check runs without them, whatever the shell has set.
TOOLS=${HORIZON_TOOLS:-../horizon/target/release}
unset HORIZON_TOOLS

# The made-up mods, compiled against the stand-ins. Their jars hold their own classes and resources only.
javac --release 21 -d "$B/stubs" $(find test/fixture/stubs -name '*.java')
javac --release 21 -cp "$B/stubs" -d "$B/mod" $(find "$F/src" -name '*.java')
javac --release 21 -cp "$B/stubs" -d "$B/odd" $(find test/fixture/oddities/src -name '*.java')
javac --release 21 -cp "$B/stubs" -d "$B/tidy" $(find test/fixture/tidy/src -name '*.java')
# Classes laid over the first mod's, for one test each.
javac --release 21 -cp "$B/stubs:$B/mod" -d "$B/stray" $(find test/fixture/extras/stray -name '*.java')
javac --release 21 -cp "$B/stubs:$B/mod" -d "$B/screen" $(find test/fixture/extras/screen -name '*.java')
javac --release 21 -cp "$B/stubs:$B/mod" -d "$B/wrapped" $(find test/fixture/extras/wrapped -name '*.java')

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

# is_file <path> / no_file <path>: the file is there, or isn't.
is_file() {
  if [ -f "$1" ]; then
    echo "  ok: $1"
  else
    echo "  FAIL: there is no $1"
    failures=$((failures + 1))
  fi
}
no_file() {
  if [ -e "$1" ]; then
    echo "  FAIL: $1 is there"
    failures=$((failures + 1))
  else
    echo "  ok: no $1"
  fi
}

# none_in <folder> <text>: no file under the folder holds the text.
none_in() {
  if grep -rqF -- "$2" "$1"; then
    echo "  FAIL: '$2' is in $(grep -rlF -- "$2" "$1" | head -3 | tr '\n' ' ')"
    failures=$((failures + 1))
  else
    echo "  ok: no '$2' under $1"
  fi
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
jar --create --file "$B/jars/tidy.jar" -C "$B/tidy" . -C test/fixture/tidy/resources .

echo; echo "== unit tests"
javac --release 21 -Xlint:all -Werror -cp build/classes -d "$B/classes" test/java/horizonneo/*.java
for t in TomlTest JsonTest MetadataTest LicenceCheckTest ClassDataTest AnalyzerTest ReportTest StandInTest BundleTest EventTableTest WiringTest ContentTest RustTextTest PackTest DumpTest DeclarationsTest MachinesTest TranslatorTest; do
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

echo; echo "== port"
H=test/fixture/horizon-src
P=$B/port
exits 2 tool port "$J"
says "$ERR" 'needs --horizon-src'
if [ -f "${HORIZON_PORT_JAR:-}" ]; then
  # The made-up mod, against the stand-in for Horizon's source.
  exits 0 tool port "$J" --horizon-src "$H" --out "$P/pc"
  says "$OUT" "Pocket Compass 1.2.3, a NeoForge mod: bundle written to $P/pc"
  says "$OUT" 'Horizon: commit 0123456789abcdef0123456789abcdef01234567 (its .horizon-revision)'
  says "$OUT" 'Injections: 3 on (list, tick, destroy-block), 13 written commented out (settings, click, minecraft-mixin-no-screen, client-events-on-hud,'
  says "$OUT" 'Mixin handlers: 2, 1 of them on'
  says "$OUT" 'Java translated to Rust: 3 of 21 methods, 1 of 14 static fields'
  says "$OUT" 'Event listeners: 12, 2 on a hook point, 10 written commented out'
  says "$OUT" "Content: no content mod: Horizon's tools for it aren't built"
  says "$OUT" 'Not handed to horizon-port: the Mixin class UnlistedMixin'
  says "$OUT" 'have been deleted. Nothing was compiled or run.'
  M=$P/pc/mixins/pocketcompass
  for f in PORTING.md Cargo.toml compatibility.json report/report.md report/report.json report/translation.md report/horizon-port-report.md \
           mixins/pocketcompass/horizon-mixin.toml mixins/pocketcompass/runtime.rs mixins/pocketcompass/tick.rs mixins/pocketcompass/list.rs \
           mixins/pocketcompass/destroy-block.rs \
           todo/settings.rs todo/click.rs todo/minecraft-mixin-no-screen.rs; do
    is_file "$P/pc/$f"
  done
  # The decompiled Java: not in the bundle, in comments or as files, and not left anywhere else.
  says "$M/tick.rs" "Its Java is com/example/pocketcompass/mixin/MinecraftMixin.java in the mod (decompiled code isn't kept in this bundle)."
  none_in "$P/pc" '// |'
  none_in "$P/pc" 'CallbackInfo'
  none_in "$P/pc" 'Decompiled Java,'
  left=$(find "$B" \( -name '*.java' -o -name '*-src' -o -name '*.hnmod' \) | wc -l)
  if [ "$left" = 0 ]; then echo "  ok: no Java source, decompiled folder or .hnmod under $B"; else echo "  FAIL: $left of them under $B"; failures=$((failures + 1)); fi
  # What horizon-port switched on by a class's name is commented out, with why.
  no_file "$M/settings.rs"
  says "$M/horizon-mixin.toml" '# Off: the `settings` hook point (the mod'"'"'s settings page), turned off by horizon-port-neoforge.'
  says "$M/horizon-mixin.toml" '# id = "click"'
  says "$M/horizon-mixin.toml" 'target = "horizon_client::game::native_hooks::Game<'"'"'_>::native_tick"'
  says "$P/pc/PORTING.md" 'generated by horizon-port from `pocketcompass.jar`'
  says "$P/pc/PORTING.md" '3 injections are on; 13 are written commented out'
  says "$P/pc/PORTING.md" '| hook `settings` | off | Commented out by horizon-port-neoforge'
  says "$P/pc/PORTING.md" '| hook `tick` | on |'
  says "$P/pc/PORTING.md" '## From a NeoForge mod'
  says "$P/pc/PORTING.md" "**No content mod was made**: Horizon's tools for it aren't built"
  says "$P/pc/PORTING.md" "- 2 event listeners are on hook points, but their handlers aren't ported"
  says "$P/pc/PORTING.md" "- 10 event listeners aren't on a hook point"
  says "$P/pc/PORTING.md" '- 6 things registered with `DeferredRegister`'
  says "$P/pc/PORTING.md" 'TODO: if the mod has settings to show, fill in todo/settings.rs'
  says "$P/pc/PORTING.md" 'Declared in META-INF/neoforge.mods.toml: `MIT`'
  says "$P/pc/PORTING.md" '- Authors: Mine, Second Author'
  lacks "$P/pc/PORTING.md" 'fabric.mod.json'
  lacks "$P/pc/PORTING.md" 'mixins/pocketcompass/settings.rs'
  says "$P/pc/Cargo.toml" 'ported from NeoForge to a Horizon native mod'
  says "$P/pc/report/report.md" '| `MinecraftMixin.pocketcompass$onTick` | auto | HEAD on hook `tick` | on, in `tick` |'
  says "$P/pc/report/report.md" '| `MinecraftMixin.pocketcompass$noScreen` | needs-translation | WRAP_OPERATION on hook `tick` | commented out, as `minecraft-mixin-no-screen` |'
  says "$P/pc/report/report.md" 'Java translated to Rust: 3 of 21 methods and 1 of 14 static fields'
  says "$P/pc/report/report.md" 'the source it decompiled for that has been deleted'
  says "$P/pc/report/report.json" '"injectionsOn": ['
  says "$P/pc/report/report.json" '"bundle": "on"'
  says "$P/pc/report/report.json" '"tool": "horizon-port-neoforge"'

  # The listeners. On: the catalogue names one hook point for the event, with nothing more to read.
  says "$M/horizon-mixin.toml" 'target = "horizon_client::game::native_hooks::Game<'"'"'_>::native_destroy_block"'
  says "$M/destroy-block.rs" "fn pocketcompass_destroy_block(game: &mut crate::game::Game<'_>, pos: [i32; 3], canceled: &mut bool) {"
  says "$M/destroy-block.rs" '//   PocketCompass.onBreak, which listens to BlockEvent.BreakEvent (addListener in PocketCompass'"'"'s constructor)'
  says "$M/destroy-block.rs" '    // TODO: port PocketCompass.onBreak here.'
  says "$M/destroy-block.rs" '    let _ = (game, pos, canceled, &m::ID);'
  # A hook point a Mixin is on already keeps its one injection.
  says "$M/tick.rs" "// Added by horizon-port-neoforge: Horizon's hook catalogue names this hook point for ClientTickEvent.Post too."
  says "$M/tick.rs" '// TODO: port ClientEvents.onClientTick here as well.'
  # Off, each with its reason: a note to read, a shared hook point, another phase only, one being prepared,
  # one said not to be its counterpart, one with no function in the source, and nothing at all.
  says "$M/horizon-mixin.toml" "# Off: ServerEvents.onLogin listens to NeoForge's PlayerEvent.PlayerLoggedInEvent."
  says "$M/horizon-mixin.toml" 'with a note to read first: "once the player is in the world, not before". `--allow-approximate` switches such listeners on.'
  says "$M/horizon-mixin.toml" 'and for other events too ("ServerTickEvent.Pre / .Post")'
  says "$M/horizon-mixin.toml" 'only for RenderGuiEvent.Pre (`gui_render_pre`)'
  says "$M/horizon-mixin.toml" 'Horizon is preparing `client_chat_received` for it (tools/native-hook-fragments/client_chat.json)'
  says "$M/horizon-mixin.toml" '`key_press` is "none (InputEvent.Key hears every key'
  says "$M/horizon-mixin.toml" 'but horizon-port read no signature for `register_key_mappings` in this Horizon source'
  says "$M/horizon-mixin.toml" "# Off: ClientEvents.onClientSetup listens to NeoForge's FMLClientSetupEvent."
  says "$M/horizon-mixin.toml" '# id = "pocket-compass-client-listener"'
  no_file "$M/server-tick.rs"
  no_file "$M/player-logged-in.rs"
  lacks "$M/horizon-mixin.toml" 'ClientEvents.notStatic'
  says "$P/pc/PORTING.md" '| `PocketCompass.onBreak` | BlockEvent.BreakEvent | `destroy_block` | on, in `destroy-block` |'
  says "$P/pc/PORTING.md" '| `ClientEvents.onClientTick` | ClientTickEvent.Post | `tick` | on, in `tick` |'
  says "$P/pc/PORTING.md" '| `ClientEvents.onHud` | RenderGuiEvent.Post |  | off, as `client-events-on-hud` |'
  says "$P/pc/report/report.md" 'Event listeners on a hook point: 2 of 12'
  says "$P/pc/report/report.md" '| `PocketCompass.onBreak` | BlockEvent.BreakEvent | exact | `destroy_block` | on, in `destroy-block` |'
  says "$P/pc/report/report.md" '| `ServerEvents.onLogin` | PlayerEvent.PlayerLoggedInEvent | approximate | `player_logged_in` | commented out, as `server-events-on-login` |'
  says "$P/pc/report/report.md" '| `ClientEvents.onChat` | ClientChatReceivedEvent | pending | `client_chat_received` |'
  says "$P/pc/report/report.json" '"level": "exact"'
  says "$P/pc/report/report.json" '"listenersOn": 2'

  # With --allow-approximate, the two whose hook point comes with something to read are on too.
  exits 0 tool port "$J" --horizon-src "$H" --out "$P/loose" --allow-approximate
  says "$OUT" 'Event listeners: 12, 4 on a hook point, 8 written commented out'
  says "$P/loose/mixins/pocketcompass/player-logged-in.rs" "fn pocketcompass_player_logged_in(game: &crate::game::Game<'_>, name: &str) {"
  says "$P/loose/mixins/pocketcompass/server-tick.rs" "fn pocketcompass_server_tick(game: &mut crate::game::Game<'_>, after: bool) {"
  says "$P/loose/report/report.md" '| `ServerEvents.onLogin` | PlayerEvent.PlayerLoggedInEvent | approximate | `player_logged_in` | on, in `player-logged-in` |'
  none_in "$P/loose" '// |'

  # A Horizon source with no hook catalogue: the listeners are left, and it says so.
  cp -r "$H" "$B/horizon-bare"
  rm "$B/horizon-bare/native-hooks.json"
  exits 0 tool port "$J" --horizon-src "$B/horizon-bare" --out "$P/bare"
  says "$OUT" 'Event listeners: 12, none on a hook point (this Horizon source has no native-hooks.json)'
  says "$OUT" 'Injections: 2 on (list, tick), 3 written commented out (settings, click, minecraft-mixin-no-screen)'
  says "$P/bare/PORTING.md" "- 12 event listeners aren't on a hook point: the Horizon source given has no hook catalogue"

  # A bundle that is there isn't written over.
  echo "edited by hand" >> "$M/tick.rs"
  exits 1 tool port "$J" --horizon-src "$H" --out "$P/pc"
  says "$ERR" 'is already there'
  says "$M/tick.rs" 'edited by hand'

  # A licence that doesn't allow it: refused before anything is decompiled, and nothing is written.
  exits 3 tool port "$B/jars/arr.jar" --horizon-src "$H" --out "$P/arr"
  says "$OUT" '**Not allowed**'
  says "$OUT" 'Nothing was decompiled and nothing was written.'
  no_file "$P/arr"
  exits 0 tool port "$B/jars/arr.jar" --horizon-src "$H" --out "$P/arr-author" --i-am-the-author "second author"
  is_file "$P/arr-author/PORTING.md"

  # A Mixin class no config lists is never applied, so it isn't converted either.
  fixture_jar stray "$B/stray"
  exits 0 tool port "$B/jars/stray.jar" --horizon-src "$H" --out "$P/stray"
  says "$OUT" 'Not handed to horizon-port: the Mixin classes StrayMixin, UnlistedMixin'
  says "$OUT" 'Injections: 3 on (list, tick, destroy-block), 13 written commented out'
  none_in "$P/stray/mixins" 'pocketcompass$stray'
  none_in "$P/stray/todo" 'pocketcompass$stray'
  says "$P/stray/report/report.md" 'StrayMixin'

  # The mod says it has a config screen: the settings page stays on.
  fixture_jar screen "$B/screen"
  exits 0 tool port "$B/jars/screen.jar" --horizon-src "$H" --out "$P/screen"
  says "$OUT" 'Injections: 5 on (list, tick, settings, click, destroy-block), 11 written commented out (minecraft-mixin-no-screen,'
  says "$OUT" 'is left on: the mod names NeoForge'"'"'s `IConfigScreenFactory` in ConfigScreens'
  is_file "$P/screen/mixins/pocketcompass/settings.rs"
  says "$P/screen/PORTING.md" '| hook `settings` | on |'

  # A Horizon that doesn't have the hook point horizon-port's table names: off, with why. And no commit known.
  cp -r "$H" "$B/horizon-other"
  rm "$B/horizon-other/.horizon-revision"
  sed -i '/fn native_tick/d' "$B/horizon-other/crates/horizon-client/src/game/native_hooks.rs"
  exits 0 tool port "$J" --horizon-src "$B/horizon-other" --out "$P/other"
  says "$OUT" "Horizon: its commit isn't known"
  says "$OUT" 'Injections: 2 on (list, destroy-block), 15 written commented out (tick, settings, click, minecraft-mixin-no-screen,'
  says "$OUT" 'Event listeners: 12, 1 on a hook point, 11 written commented out'
  says "$P/other/mixins/pocketcompass/horizon-mixin.toml" 'but horizon-port read no signature for `tick` in this Horizon source'
  says "$OUT" 'Mixin handlers: 2, 0 of them on'
  says "$OUT" 'The injection `tick` is commented out'
  says "$P/other/mixins/pocketcompass/horizon-mixin.toml" 'crates/horizon-client/src/game/native_hooks.rs` has no `fn native_tick`'
  is_file "$P/other/todo/tick.rs"
  no_file "$P/other/mixins/pocketcompass/tick.rs"
  says "$P/other/PORTING.md" '| hook `tick` | off |'
  says "$P/other/report/report.md" '| `MinecraftMixin.pocketcompass$onTick` | auto | HEAD on hook `tick` | commented out, as `tick` |'
  says "$P/other/report/report.md" '| needs-translation | Written commented out, as `tick`.'

  # The commit: given, wrong, or git's.
  exits 0 tool port "$J" --horizon-src "$B/horizon-other" --out "$P/given" --horizon-revision 1111111111111111111111111111111111111111
  says "$OUT" 'Horizon: commit 1111111111111111111111111111111111111111 (given)'
  says "$P/given/compatibility.json" '"horizon_revision": "1111111111111111111111111111111111111111"'
  exits 1 tool port "$J" --horizon-src "$H" --out "$P/short" --horizon-revision abc123
  says "$ERR" 'takes a full commit'
  no_file "$P/short"
  if command -v git > /dev/null; then
    cp -r "$B/horizon-other" "$B/horizon-git"
    git -C "$B/horizon-git" init -q
    git -C "$B/horizon-git" add -A
    git -C "$B/horizon-git" -c user.name=test -c user.email=test@example.com commit -q -m "a stand-in"
    head=$(git -C "$B/horizon-git" rev-parse HEAD)
    exits 0 tool port "$J" --horizon-src "$B/horizon-git" --out "$P/git"
    says "$OUT" "Horizon: commit $head (git's HEAD of that folder)"
    says "$P/git/compatibility.json" "\"horizon_revision\": \"$head\""
    echo "// changed" >> "$B/horizon-git/crates/horizon-client/src/game.rs"
    exits 0 tool port "$J" --horizon-src "$B/horizon-git" --out "$P/git-changed"
    says "$OUT" "it has changes that aren't committed"
  fi

  # A mod horizon-port sees nothing left to do in: its .hnmod isn't kept.
  exits 0 tool port "$B/jars/tidy.jar" --horizon-src "$H" --out "$P/tidy"
  says "$OUT" 'Injections: 1 on (list), 0 written commented out'
  says "$OUT" 'Event listeners: none'
  says "$OUT" 'Content: no content mod, the jar holds no `assets/` and no `data/`'
  says "$P/tidy/PORTING.md" '**Not packed.**'
  lacks "$P/tidy/PORTING.md" 'Yes: this is a full port'
  says "$P/tidy/PORTING.md" "Nothing of the mod's NeoForge side is known to be left out."
  no_file "$P/tidy/tidy.hnmod"

  # The second mod: a jar inside the jar isn't handed over.
  exits 0 tool port "$B/jars/oddities.jar" --horizon-src "$H" --out "$P/odd"
  says "$OUT" 'Not handed to horizon-port: 1 jar inside the jar (META-INF/jarjar/library-1.0.jar)'

  # Not Horizon's source, not a jar.
  exits 1 tool port "$J" --horizon-src "$B/no-such-folder" --out "$P/nowhere"
  says "$ERR" "isn't Horizon's source folder"
  exits 1 tool port "$B/jars/forge.jar" --horizon-src "$H" --out "$P/nowhere"
  no_file "$P/nowhere"

  # A Fabric jar is horizon-port's own job, as it is: its bundle, with nothing of this tool's in it.
  mkdir -p "$B/cwd-fabric"
  (cd "$B/cwd-fabric" && exits_here=0 && java -Djava.io.tmpdir="$OLDPWD/$B/tmp" -jar "$OLDPWD/build/horizon-port-neoforge.jar" port "$OLDPWD/$B/jars/fabric.jar" \
      --horizon-src "$OLDPWD/$H" --out fabric-bundle > stdout.txt 2> stderr.txt) && echo "  ok: exit 0: port of a Fabric jar" || { echo "  FAIL: port of a Fabric jar"; failures=$((failures + 1)); }
  says "$B/cwd-fabric/stdout.txt" 'Bundle: fabric-bundle'
  lacks "$B/cwd-fabric/stdout.txt" 'a NeoForge mod'
  lacks "$B/cwd-fabric/fabric-bundle/PORTING.md" '## From a NeoForge mod'
  rm -rf "$B/cwd-fabric"
fi

echo; echo "== content"
if [ -f "${HORIZON_PORT_JAR:-}" ]; then
  # A carry that fails: the bundle is still written, and says why it has no content mod.
  mkdir -p "$B/tools-broken" "$B/tools-none"
  printf '#!/bin/sh\necho "error: boom" >&2\nexit 1\n' > "$B/tools-broken/horizon-port-pack"
  cp "$B/tools-broken/horizon-port-pack" "$B/tools-broken/cargo-horizon-mod"
  chmod +x "$B/tools-broken/horizon-port-pack" "$B/tools-broken/cargo-horizon-mod"
  exits 0 tool port "$J" --horizon-src "$H" --out "$P/broken-tools" --horizon-tools "$B/tools-broken"
  says "$OUT" 'Content: no content mod: horizon-port-pack refused the jar: boom'
  says "$P/broken-tools/PORTING.md" '**No `.hmod` was made**: horizon-port-pack refused the jar: boom.'
  # A folder that is named and doesn't hold the tools: refused before anything is done.
  exits 1 tool port "$J" --horizon-src "$H" --out "$P/no-tools" --horizon-tools "$B/tools-none"
  says "$ERR" "doesn't hold horizon-port-pack and cargo-horizon-mod"
  no_file "$P/no-tools"

  if [ -x "$TOOLS/horizon-port-pack" ] && [ -x "$TOOLS/cargo-horizon-mod" ]; then
    T=$(cd "$TOOLS" && pwd)
    # Data and assets only: packed, and nothing in it is a guess.
    exits 0 tool port "$J" --horizon-src "$H" --out "$P/content" --horizon-tools "$T"
    says "$OUT" 'Content: pocketcompass.hmod (1 file), packed and checked by `cargo horizon-mod`; it declares no block or item, so nothing in it is a guess'
    is_file "$P/content/pocketcompass.hmod"
    is_file "$P/content/content/horizon-mod.toml"
    is_file "$P/content/content/assets/pocketcompass/lang/en_us.json"
    no_file "$P/content/content/target"
    jar tf "$P/content/pocketcompass.hmod" > "$B/out/hmod.txt"
    says "$B/out/hmod.txt" 'horizon-mod.toml'
    says "$B/out/hmod.txt" 'assets/pocketcompass/lang/en_us.json'
    lacks "$B/out/hmod.txt" '.class'
    # Its recipe makes an item the content mod doesn't declare: the carry keeps it out, and that is said.
    lacks "$B/out/hmod.txt" 'recipe'
    is_file "$P/content/todo/content-left-out/data/pocketcompass/recipe/pocket_compass.json"
    says "$P/content/PORTING.md" "The mod's data and assets are in **\`pocketcompass.hmod\`**, a content mod (1 file; its folder is \`content/\`)"
    says "$P/content/PORTING.md" "a recipe whose result is an item of the mod's that the content mod doesn't declare"
    says "$P/content/PORTING.md" 'need a content mod (.hmod): `pocketcompass.hmod` (see "From a NeoForge mod").'
    says "$P/content/PORTING.md" 'block `pocketcompass:spinning_block`'
    says "$P/content/report/report.json" '"state": "packed"'
    says "$P/content/report/report.md" '- Content: `pocketcompass.hmod`, packed and checked'

    # A carry that declares a block and an item copies vanilla ones by guess: left as a folder to check.
    fixture_jar content test/fixture/extras/content
    exits 0 tool port "$B/jars/content.jar" --horizon-src "$H" --out "$P/guessed" --horizon-tools "$T"
    says "$OUT" "Content: not packed: it declares 1 block and 1 item, each copying a vanilla one that is Horizon's guess."
    no_file "$P/guessed/pocketcompass.hmod"
    no_file "$P/guessed/content"
    is_file "$P/guessed/todo/content/horizon-mod.toml"
    is_file "$P/guessed/todo/content/data/pocketcompass/horizon/block/plain_block.json"
    is_file "$P/guessed/todo/content/data/pocketcompass/recipe/pocket_compass.json"
    says "$P/guessed/PORTING.md" '| block `pocketcompass:plain_block` | `minecraft:stone` | yes |'
    says "$P/guessed/PORTING.md" '| item `pocketcompass:pocket_compass` | `minecraft:stick` | yes |'
    says "$P/guessed/PORTING.md" 'TODO: check each declaration below against the mod'
    says "$P/guessed/PORTING.md" "- The content mod isn't packed: its folder is \`todo/content/\`, with declarations to check first."
    says "$P/guessed/PORTING.md" 'need a content mod (.hmod), which isn'"'"'t made (see "From a NeoForge mod").'
    says "$P/guessed/report/report.md" "| needs-mapping | Declared in \`todo/content/\`, which isn't packed, as a copy of \`minecraft:stone\`: Horizon's guess"
    says "$P/guessed/report/report.md" "| needs-translation | The content mod doesn't declare it"
    says "$P/guessed/report/report.json" '"state": "todo"'

    # A block registered through the mod's own register wrapper: whether the code registers it isn't known.
    mkdir -p "$B/wrapped-content"
    cp -r test/fixture/extras/content/. test/fixture/extras/wrapped-assets/. "$B/wrapped/." "$B/wrapped-content/"
    fixture_jar wrapped "$B/wrapped-content"
    exits 0 tool port "$B/jars/wrapped.jar" --horizon-src "$H" --out "$P/wrapped" --horizon-tools "$T"
    says "$P/wrapped/PORTING.md" '| block `pocketcompass:wrapped_block` | `minecraft:stone` | not read: some of the mod'"'"'s registers weren'"'"'t read'
    says "$P/wrapped/PORTING.md" '| block `pocketcompass:plain_block` | `minecraft:stone` | yes |'
    lacks "$P/wrapped/PORTING.md" "the code doesn't register it"
    says "$P/wrapped/report/report.md" "makes a DeferredRegister that isn't kept in a static field"

    # With the mod's facts, horizon-port-pack's pipeline declares each block and item from them: packed, nothing guessed.
    mkdir -p "$B/machine-content"
    cp -r test/fixture/extras/content/. test/fixture/extras/machine/. "$B/machine-content/"
    fixture_jar machine "$B/machine-content"
    exits 0 tool port "$B/jars/machine.jar" --horizon-src "$H" --out "$P/facts" --horizon-tools "$T" --facts test/fixture/facts/pocketcompass.json
    says "$OUT" 'Content: pocketcompass.hmod'
    says "$OUT" "it declares 2 blocks and 1 item, each from the mod's facts in NeoForge"
    is_file "$P/facts/pocketcompass.hmod"
    no_file "$P/facts/todo/content/horizon-mod.toml"
    says "$P/facts/content/data/pocketcompass/horizon/block/plain_block.json" '"hardness": 5.0'
    says "$P/facts/content/data/pocketcompass/horizon/block/plain_block.json" '"light": 7'
    says "$P/facts/content/data/pocketcompass/horizon/item/pocket_compass.json" '"max_stack_size": 1'
    says "$P/facts/PORTING.md" '| block `pocketcompass:plain_block` | `minecraft:stone` | yes |'
    lacks "$P/facts/PORTING.md" "Horizon's guess"
    # The machine no vanilla template takes: declared with state properties of its own, its recipe back.
    says "$P/facts/content/data/pocketcompass/horizon/block/machine_block.json" '"state_properties"'
    says "$P/facts/content/data/pocketcompass/horizon/block/machine_block.json" '"active": "false"'
    says "$P/facts/content/data/pocketcompass/horizon/block/machine_block.json" '"state_light"'
    says "$P/facts/content/data/pocketcompass/horizon/block/machine_block.json" '"minecraft:rarity": "uncommon"'
    is_file "$P/facts/content/data/pocketcompass/recipe/machine_block.json"
    no_file "$P/facts/todo/content-left-out/data/pocketcompass/recipe/machine_block.json"
    says "$P/facts/PORTING.md" '| block `pocketcompass:machine_block` | `minecraft:stone` | yes |'
    says "$P/facts/PORTING.md" '- `pocketcompass:machine_block`: declared by this tool on minecraft:stone, with state properties of its own (active, facing)'
    says "$P/facts/PORTING.md" 'class com.example.pocketcompass.MachineBlock (behaviour beyond net.minecraft.world.level.block.Block): its code isn'"'"'t ported'
    lacks "$P/facts/PORTING.md" "aren't declared. horizon-port-pack declares only"
    # The machines file beside the facts: the block entity on the hook points the fixture's Horizon has, what was measured, the rest said.
    says "$OUT" "Machines: 1 blocks with a block entity; 5 block-entity hook points on, 1 written commented out"
    M="$P/facts/mixins/pocketcompass"
    says "$M/block-entity.rs" '"pocketcompass:machine_block" => "pocketcompass:machine",'
    says "$M/block-entity.rs" 'horizon_world::nbt::Compound::new()'
    says "$M/block-entity-ticks.rs" '*ticks = true;'
    says "$M/block-entity-tick.rs" "TODO: port each block entity's tick"
    says "$M/block-entity-tick.rs" "pocketcompass:machine: com.example.pocketcompass.MachineBlockEntity; it saves energy, progress"
    says "$M/block-entity-container.rs" 'faces: [vec![2], vec![0], Vec::new(), Vec::new(), Vec::new(), Vec::new()],'
    says "$M/block-entity-container.rs" 'inputs: vec![0],'
    says "$M/block-entity-container.rs" 'outputs: vec![2],'
    says "$M/block-entity-energy.rs" 'max_insert: 500,'
    says "$M/block-entity-energy.rs" 'insert_faces: [false, false, false, true, false, false],'
    says "$M/horizon-mixin.toml" 'target = "horizon_client::game::native_hooks::Game<'"'"'_>::native_block_entity_container"'
    says "$M/horizon-mixin.toml" "# Off: the mod's machines on \`block_entity_fluids\`"
    says "$M/horizon-mixin.toml" "This Horizon source's catalogue has no \`block_entity_fluids\`"
    says "$P/facts/compatibility.json" '"hooks": 142'
    says "$P/facts/PORTING.md" '| `block_entity_container` | 1 | on, in `block-entity-container` |'
    says "$P/facts/report/machines.md" '| `pocketcompass:machine_block` | `pocketcompass:machine` | yes | energy, progress |'
    says "$P/facts/report/machines.md" "1 face slot took none of the items tried"
    says "$P/facts/report/machines.md" "neither the block nor its block entity hands NeoForge a MenuProvider"
    exits 0 tool check "$P/facts" --horizon-src "$H"
    says "$OUT" "every target is in this Horizon's source"

    # With --allow-approximate it is packed as it is, and each guess is still listed.
    exits 0 tool port "$B/jars/content.jar" --horizon-src "$H" --out "$P/guessed-packed" --horizon-tools "$T" --allow-approximate
    says "$OUT" 'Content: pocketcompass.hmod (8 files), packed and checked by `cargo horizon-mod`; it declares 1 block and 1 item'
    jar tf "$P/guessed-packed/pocketcompass.hmod" > "$B/out/hmod.txt"
    says "$B/out/hmod.txt" 'data/pocketcompass/horizon/block/plain_block.json'
    says "$B/out/hmod.txt" 'data/pocketcompass/horizon/item/pocket_compass.json'
    says "$B/out/hmod.txt" 'data/pocketcompass/recipe/pocket_compass.json'
    no_file "$P/guessed-packed/todo/content"
    says "$P/guessed-packed/PORTING.md" '| block `pocketcompass:plain_block` | `minecraft:stone` | yes |'
    says "$P/guessed-packed/PORTING.md" '6 things are registered with `DeferredRegister`: 2 are declared in `pocketcompass.hmod`, copying a vanilla template to check'
    says "$P/guessed-packed/report/report.md" "| \`pocketcompass.hmod\` | content | Declared in \`pocketcompass.hmod\`, as a copy of \`minecraft:stick\`: Horizon's guess"

    # The packing tool refuses: no .hmod, the folder is kept to pack by hand.
    mkdir -p "$B/tools-half"
    ln -sf "$T/horizon-port-pack" "$B/tools-half/horizon-port-pack"
    cp "$B/tools-broken/cargo-horizon-mod" "$B/tools-half/cargo-horizon-mod"
    exits 0 tool port "$J" --horizon-src "$H" --out "$P/half" --horizon-tools "$B/tools-half"
    says "$OUT" 'Content: no content mod: cargo horizon-mod build refused it: boom'
    no_file "$P/half/pocketcompass.hmod"
    is_file "$P/half/todo/content/horizon-mod.toml"
    says "$P/half/PORTING.md" 'The folder it was to be packed from is `todo/content/`.'
  else
    echo "  (skipped: the checks that pack a .hmod. Horizon's content tools aren't in $TOOLS: build them in Horizon's source,"
    echo "   cargo build --release -p horizon-port-pack -p cargo-horizon-mod, or set HORIZON_TOOLS to their folder)"
  fi
fi

echo; echo "== check"
exits 2 tool check "$B/port/pc"
says "$ERR" 'needs --horizon-src'
exits 1 tool check "$B/nowhere" --horizon-src "$H"
says "$ERR" "isn't a folder"
if [ -f "${HORIZON_PORT_JAR:-}" ]; then
  C=$B/check
  mkdir -p "$C" "$B/fake"
  # The bundle `port` wrote, by reading alone: nothing is compiled, and nothing is written into it.
  exits 0 tool check "$P/pc" --horizon-src "$H"
  says "$OUT" 'the mod `pocketcompass`, 3 injections on, 13 written commented out'
  says "$OUT" 'ok: 2 modules: each file is there, with nothing horizon-mixins refuses in a module'
  says "$OUT" 'ok: 3 injections on: each has its helper, one function with a parameter for each binding'
  says "$OUT" "ok: every target is in this Horizon's source: 3 hook points, each binding naming something it takes"
  says "$OUT" 'ok: hooks API 1 covers its hook points (they need 1; this Horizon has 142)'
  says "$OUT" 'ok: it was made for this Horizon commit (0123456789abcdef0123456789abcdef01234567)'
  says "$OUT" 'ok: the brackets pair up in the 5 Rust files that are on'
  says "$OUT" 'ok: no decompiled Java, class file or jar is in it'
  says "$OUT" 'Left to do: 3 TODOs in 2 files of the code that is on, 13 injections written commented out'
  says "$OUT" 'Nothing was compiled.'
  says "$OUT" 'RESULT: ok, not compiled'
  lacks "$OUT" 'problem:'
  no_file "$P/pc/target"

  # broken <name>: a copy of that bundle to spoil, in $C/<name>.
  broken() { rm -rf "$C/$1"; cp -r "$P/pc" "$C/$1"; }
  CM=mixins/pocketcompass

  broken helper-gone
  rm "$C/helper-gone/$CM/tick.rs"
  exits 1 tool check "$C/helper-gone" --horizon-src "$H"
  says "$OUT" 'problem: the injection `tick` names the helper tick.rs, which isn'"'"'t there'
  says "$OUT" 'RESULT: failed (1 problem)'

  broken two-functions
  printf '\nfn another() {}\n' >> "$C/two-functions/$CM/tick.rs"
  exits 1 tool check "$C/two-functions" --horizon-src "$H"
  says "$OUT" 'problem: tick.rs holds 2 functions outside any block: a helper is one function'

  broken parameter
  sed -i 's/^fn pocketcompass_tick(game: /fn pocketcompass_tick(extra: u8, game: /' "$C/parameter/$CM/tick.rs"
  exits 1 tool check "$C/parameter" --horizon-src "$H"
  says "$OUT" 'problem: tick.rs: `fn pocketcompass_tick` takes 2 parameters, and the injection `tick` has 1 binding'

  broken bracket
  printf '\nfn open() {\n' >> "$C/bracket/$CM/runtime.rs"
  exits 1 tool check "$C/bracket" --horizon-src "$H"
  says "$OUT" "problem: $CM/runtime.rs, line "
  says "$OUT" '`{` is never closed'

  broken target
  sed -i 's/::native_tick"/::native_tock"/' "$C/target/$CM/horizon-mixin.toml"
  exits 1 tool check "$C/target" --horizon-src "$H"
  says "$OUT" 'problem: the injection `tick` goes into `horizon_client::game::native_hooks::Game<'"'"'_>::native_tock`, and `crates/horizon-client/src/game/native_hooks.rs` has no `fn native_tock`'

  broken binding
  sed -i 's/value = "&mut \*canceled"/value = "\&mut *cancelled"/' "$C/binding/$CM/horizon-mixin.toml"
  exits 1 tool check "$C/binding" --horizon-src "$H"
  says "$OUT" 'problem: the injection `destroy-block` binds `&mut *cancelled`, and its hook point takes no `cancelled` in this Horizon (it takes self, pos, canceled)'

  broken api
  sed -i 's/"hooks": 1,/"hooks": 700,/' "$C/api/compatibility.json"
  exits 1 tool check "$C/api" --horizon-src "$H"
  says "$OUT" 'problem: compatibility.json asks for hooks API 700, and this Horizon has API 142'

  broken module
  printf '\nmod other;\n' >> "$C/module/$CM/runtime.rs"
  exits 1 tool check "$C/module" --horizon-src "$H"
  says "$OUT" 'problem: runtime.rs has a `mod x;` naming another file: only inline `mod x { ... }` works in a module'

  broken java
  printf '    // | this.minecraft.setScreen(null);\n' >> "$C/java/$CM/tick.rs"
  printf 'x' > "$C/java/Mod.class"
  exits 1 tool check "$C/java" --horizon-src "$H"
  says "$OUT" "problem: $CM/tick.rs quotes decompiled Java (1 line starting with \`// |\`)"
  says "$OUT" "problem: Mod.class: the mod's own code doesn't belong in a bundle"
  says "$OUT" 'RESULT: failed (2 problems)'
  rm "$C/java/Mod.class"

  broken manifest
  printf '\n[[injection\n' >> "$C/manifest/$CM/horizon-mixin.toml"
  exits 1 tool check "$C/manifest" --horizon-src "$H"
  says "$OUT" "problem: horizon-mixin.toml can't be read"
  exits 1 tool check "$B/out" --horizon-src "$H"
  says "$OUT" "problem: it has no mixins/<mod id>/horizon-mixin.toml: it isn't a native-mod bundle"

  # Another commit of Horizon than the bundle names: said, and not a problem.
  cp -r "$H" "$B/horizon-moved"
  echo "89abcdef0123456789abcdef0123456789abcdef" > "$B/horizon-moved/.horizon-revision"
  exits 0 tool check "$P/pc" --horizon-src "$B/horizon-moved"
  says "$OUT" 'note: the bundle was made for Horizon 0123456789abcdef0123456789abcdef01234567, and this source is 89abcdef0123456789abcdef0123456789abcdef'

  # --compile hands the bundle to horizon-port's own check. Here a stand-in for it: nothing is compiled.
  javac --release 21 -d "$B/fake" test/fixture/fake-port/FakePort.java
  jar --create --file "$B/jars/fake-port.jar" --main-class FakePort -C "$B/fake" .
  exits 0 tool check "$P/pc" --horizon-src "$H" --compile --translator "$B/jars/fake-port.jar"
  says "$OUT" "not checked: the hook points' bindings and API (horizon-port couldn't list that source's hook points: fake horizon-port: it only checks)"
  says "$OUT" "    fake horizon-port: check $PWD/$P/pc --horizon-src $PWD/$H"
  says "$OUT" '    its stack: -Xss512m'
  says "$OUT" "ok: horizon-port's check passes"
  says "$OUT" 'RESULT: ok (1 not checked, above)'
  lacks "$OUT" 'Nothing was compiled.'
  exits 1 env FAKE_PORT=fail java -Djava.io.tmpdir="$PWD/$B/tmp" -jar build/horizon-port-neoforge.jar check "$P/pc" --horizon-src "$H" --compile --translator "$B/jars/fake-port.jar"
  says "$OUT" '    src/lib.rs:3:5: error[E0425]: cannot find value `x` in this scope'
  says "$OUT" "problem: horizon-port's check failed (exit 1, above)"
  says "$OUT" 'RESULT: failed (1 problem)'
  # What cargo prints as errors comes before the line that points up at it.
  if [ "$(grep -n 'error\[E0425\]' "$OUT" | cut -d: -f1)" -lt "$(grep -n "doesn't compile (above)" "$OUT" | cut -d: -f1)" ]; then
    echo "  ok: cargo's errors come before the line that follows them"
  else
    echo "  FAIL: cargo's errors come after the line that says they are above"
    failures=$((failures + 1))
  fi
  # horizon-port's check doesn't know that Horizon: said as what it is, not as a fault in the bundle's Rust.
  exits 1 env FAKE_PORT=other-horizon java -Djava.io.tmpdir="$PWD/$B/tmp" -jar build/horizon-port-neoforge.jar check "$P/pc" --horizon-src "$H" --compile --translator "$B/jars/fake-port.jar"
  says "$OUT" '      Horizon changed: `fn items(&self) -> &Items {`'
  says "$OUT" "problem: horizon-port's check stopped before compiling anything"
  lacks "$OUT" "It fails on code that uses the game's own types"
  exits 0 env FAKE_PORT=no-cargo java -Djava.io.tmpdir="$PWD/$B/tmp" -jar build/horizon-port-neoforge.jar check "$P/pc" --horizon-src "$H" --compile --translator "$B/jars/fake-port.jar"
  says "$OUT" 'not checked: the compile: there is no cargo on this machine'
  says "$OUT" 'RESULT: ok, not compiled (2 not checked, above)'
  rm -rf "$B/fake"

  # The content mod: its .hmod goes through Horizon's own check.
  if [ -x "$TOOLS/horizon-port-pack" ] && [ -x "$TOOLS/cargo-horizon-mod" ]; then
    exits 0 tool check "$P/content" --horizon-src "$H" --horizon-tools "$T"
    says "$OUT" 'ok: pocketcompass.hmod passes `cargo horizon-mod check`'
    exits 0 tool check "$P/content" --horizon-src "$H"
    says "$OUT" "not checked: pocketcompass.hmod (Horizon's cargo-horizon-mod isn't built"
    exits 0 tool check "$P/guessed" --horizon-src "$H" --horizon-tools "$T"
    says "$OUT" "note: the mod's content is a folder under todo/content/, not packed: PORTING.md says why"
    rm -rf "$C/hmod"; cp -r "$P/content" "$C/hmod"
    echo "not a zip" > "$C/hmod/pocketcompass.hmod"
    exits 1 tool check "$C/hmod" --horizon-src "$H" --horizon-tools "$T"
    says "$OUT" "problem: pocketcompass.hmod doesn't pass \`cargo horizon-mod check\`"
  else
    echo "  (skipped: the checks of a .hmod, as above)"
  fi
fi

echo; echo "== pack"
exits 2 tool pack "$B/port/pc"
says "$ERR" 'needs --horizon-src'
if [ -f "${HORIZON_PORT_JAR:-}" ]; then
  K=$B/pack
  mkdir -p "$K"
  # The bundle `port` wrote, as a .hnmod: Horizon's manifest, then the files of mixins/<id>/, the commit pinned.
  rm -rf "$K/pc"; cp -r "$P/pc" "$K/pc"
  exits 0 tool pack "$K/pc" --horizon-src "$H"
  says "$OUT" "Packed $PWD/$K/pc/pocketcompass.hnmod: 6 files of mixins/pocketcompass/, for Horizon 0123456789abcdef0123456789abcdef01234567."
  says "$OUT" 'It touches hook points only (API 1): the launcher also builds it on a later Horizon that has that API.'
  says "$OUT" "It isn't a finished port: 3 TODOs in the code that is on, 13 injections commented out."
  says "$OUT" "horizon-launcher native-build \"$PWD/$K/pc/mixins/pocketcompass\" --instance <name> --trust-native-code"
  says "$K/pc/PORTING.md" 'horizon-launcher native-build "<this folder>/mixins/pocketcompass" --instance <name> --trust-native-code'
  says "$OUT" 'Nothing was compiled or installed here.'
  says "$OUT" "not checked: the file, by Horizon's own check"
  is_file "$K/pc/pocketcompass.hnmod"
  no_file "$K/pc/pocketcompass.hnmod.part"
  no_file "$K/pc/target"
  jar tf "$K/pc/pocketcompass.hnmod" > "$B/out/hnmod.txt"
  says "$B/out/hnmod.txt" 'horizon-native.json'
  for f in horizon-mixin.toml runtime.rs game_side.rs tick.rs list.rs destroy-block.rs; do says "$B/out/hnmod.txt" "mixins/pocketcompass/$f"; done
  lacks "$B/out/hnmod.txt" 'todo/'
  lacks "$B/out/hnmod.txt" 'report/'
  lacks "$B/out/hnmod.txt" 'PORTING.md'
  if [ "$(head -1 "$B/out/hnmod.txt")" = "horizon-native.json" ]; then echo "  ok: the manifest comes first"; else echo "  FAIL: the manifest isn't the first entry"; failures=$((failures + 1)); fi
  unzip -p "$K/pc/pocketcompass.hnmod" horizon-native.json > "$B/out/native.json"
  says "$B/out/native.json" '"schema": 1'
  says "$B/out/native.json" '"id": "pocketcompass"'
  says "$B/out/native.json" '"version": "1.2.3"'
  says "$B/out/native.json" '"game": "26.3"'
  says "$B/out/native.json" '"horizon": "0123456789abcdef0123456789abcdef01234567"'
  says "$B/out/native.json" '"mixins": "89abcdef0123456789abcdef0123456789abcdef"'
  says "$B/out/native.json" '"hooks": 1'
  unzip -p "$K/pc/pocketcompass.hnmod" mixins/pocketcompass/horizon-mixin.toml > "$B/out/packed.toml"
  says "$B/out/packed.toml" '[compatibility]'
  says "$B/out/packed.toml" 'horizon_revision = "0123456789abcdef0123456789abcdef01234567"'
  # The bundle's own manifest isn't touched, and the same bundle packs to the same file.
  lacks "$K/pc/mixins/pocketcompass/horizon-mixin.toml" '[compatibility]'
  exits 0 tool pack "$K/pc" --horizon-src "$H" --out "$K/again/pc.hnmod"
  if cmp -s "$K/pc/pocketcompass.hnmod" "$K/again/pc.hnmod"; then echo "  ok: packed twice, the same file"; else echo "  FAIL: packed twice, two different files"; failures=$((failures + 1)); fi
  # It still passes `check` with its .hnmod beside it.
  exits 0 tool check "$K/pc" --horizon-src "$H"

  # Not packed: a bundle with a problem, a file Horizon doesn't take, no Horizon commit.
  rm -rf "$K/broken"; cp -r "$P/pc" "$K/broken"; rm "$K/broken/mixins/pocketcompass/tick.rs"
  exits 1 tool pack "$K/broken" --horizon-src "$H"
  says "$OUT" 'Not packed: 1 problem above.'
  no_file "$K/broken/pocketcompass.hnmod"
  rm -rf "$K/extra"; cp -r "$P/pc" "$K/extra"; echo "notes" > "$K/extra/mixins/pocketcompass/notes.txt"
  exits 1 tool pack "$K/extra" --horizon-src "$H"
  says "$OUT" "mixins/pocketcompass/ holds 1 file Horizon doesn't take in a native file (notes.txt)"
  no_file "$K/extra/pocketcompass.hnmod"
  rm -rf "$K/nocommit"; cp -r "$P/pc" "$K/nocommit"; sed -i '/"horizon_revision"/d' "$K/nocommit/compatibility.json"
  exits 1 tool pack "$K/nocommit" --horizon-src "$H"
  says "$OUT" 'the bundle names no Horizon commit (compatibility.json, `horizon_revision`)'
  no_file "$K/nocommit/pocketcompass.hnmod"

  # Horizon's own check of the file, when its tools are built.
  if [ -x "$TOOLS/horizon-port-pack" ] && [ -x "$TOOLS/cargo-horizon-mod" ]; then
    rm -rf "$K/checked"; cp -r "$P/pc" "$K/checked"
    exits 0 tool pack "$K/checked" --horizon-src "$H" --horizon-tools "$T"
    says "$OUT" "ok: Horizon's own check takes the file (\`cargo horizon-mod check\`)"
    is_file "$K/checked/pocketcompass.hnmod"
    # A mod id Horizon doesn't take in a native file.
    exits 1 tool pack "$K/checked" --horizon-src "$H" --horizon-tools "$B/tools-broken"
    says "$OUT" "Not packed: Horizon's own check refuses the file: error: boom."
  else
    echo "  (skipped: Horizon's own check of a .hnmod, as above)"
  fi
fi

echo; echo "== translate"
# This tool's own translator on a made-up class (test/translate/): what it translates, what it refuses and why,
# and, where rustc is, the translation run on the same inputs as the Java: the outputs must be the same.
TR=$B/translate
mkdir -p "$TR/classes" "$TR/jar/META-INF"
# The game's stand-ins (game/) are what the made-up classes are built against, and what the Java reference runs with.
javac --release 21 -cp build/model -d "$TR/classes" test/translate/src/com/example/numbers/*.java
javac --release 21 -cp "$TR/classes" -d "$TR/classes" test/translate/NumbersCheck.java
printf 'modLoader = "javafml"\nloaderVersion = "[4,)"\nlicense = "MIT"\n[[mods]]\nmodId = "numbers"\nversion = "1.0"\ndisplayName = "Numbers"\n' > "$TR/jar/META-INF/neoforge.mods.toml"
jar --create --file "$B/jars/numbers.jar" -C "$TR/jar" . -C "$TR/classes" com
exits 0 tool translate "$B/jars/numbers.jar" --out "$TR/out"
says "$OUT" "numbers.jar: 490 of 490 methods translated from their bytecode"
# Class objects: an enum's valueOf (Enum.valueOf with a class constant), and their tables.
says "$TR/out/translation.md" '- `com.example.numbers.Boxes$Color.valueOf(Ljava/lang/String;)Lcom/example/numbers/Boxes$Color;` as'
says "$TR/out/translated.rs" 'fn jinstance_of(t: u32, a: u32) -> bool {'
says "$TR/out/translated.rs" 'fn jnative_object_get_class(this: Ref) -> JR<Ref> {'
# A static method initializes its class first, as invokestatic does: here, for its assertions' flag.
if grep -A1 'pub fn gathered__checked(mut l0: i32) -> JR<Ref> {' "$TR/out/translated.rs" | grep -q '^    jinit_[0-9]*()?;$'; then
  echo "  ok: gathered__checked initializes its class first"
else
  echo "  FAIL: gathered__checked doesn't initialize its class first"; failures=$((failures + 1))
fi
says "$OUT" "Nothing was compiled or run"
# The game's stand-ins (game/), translated with the mod where it reaches them.
says "$OUT" "Stand-ins for the game: 26 classes, not checked against the game's (no --server)."
says "$TR/out/translation.md" '- `com.example.numbers.Places.keys(I)Ljava/lang/String;` as `places__keys`'
says "$TR/out/translated.rs" '/// `net.minecraft.core.Vec3i.compareTo(Lnet/minecraft/core/Vec3i;)I`, translated from its bytecode.'
says "$TR/out/translated.rs" 'pub fn numbers__collatz(mut l0: i64) -> JR<i64> {'
says "$TR/out/translated.rs" 'pub fn numbers__is_digit(p0: u16) -> JR<bool> {'
says "$TR/out/translated.rs" 'fn jcmpg_f32(a: f32, b: f32) -> i32 {'
lacks "$TR/out/translated.rs" 'fn jsignum_f32'
says "$TR/out/translated.rs" 'pub fn numbers__instance(mut l0: Ref) -> JR<i32> {'
says "$TR/out/translated.rs" 'static JINIT_'
says "$TR/out/translated.rs" '/// `com.example.numbers.Shapes$Area.twiceArea()I`, called virtually: the method each class selects.'
says "$TR/out/translated.rs" 'that runs `com.example.numbers.Shapes$Square.<init>(I)V`'
# Exceptions, texts, and the library model translated with the mod where it reaches it.
says "$TR/out/translated.rs" 'pub fn texts__divide(mut l0: i32, mut l1: i32) -> JR<Ref> {'
says "$TR/out/translated.rs" 'Err(e) => {'
says "$TR/out/translated.rs" '/// `java.lang.String.concat(Ljava/lang/String;)Ljava/lang/String;`, translated from its bytecode.'
says "$TR/out/translated.rs" 'fn jnative_double_text(d: f64) -> JR<Ref> {'
lacks "$TR/out/translated.rs" '/// `java.lang.String.repeat'"'"'(Ljava'
# Collections: the model's HashMap (the JDK's order), and Object's methods first on a lambda of Comparator.
says "$TR/out/translated.rs" '/// `java.util.HashMap.putVal(ILjava/lang/Object;Ljava/lang/Object;ZZ)Ljava/lang/Object;`, translated from its bytecode.'
says "$TR/out/translated.rs" 'fn jnative_array_like(a: Ref, length: i32) -> JR<Ref> {'
# The flow as Rust's own loops and labelled blocks (Ramsey's structuring), not a match on the block in a loop.
says "$TR/out/translated.rs" ": loop {"
says "$TR/out/translated.rs" "break 'b"
# A negative constant of the class file, as a whole argument: no parentheses (rustc warns of them).
says "$TR/out/translated.rs" 'Value::F(-2.5f32)'
lacks "$TR/out/translation.md" 'nothing implements equals(Ljava/lang/Object;)Z on java.util.Comparator'
lacks "$TR/out/translation.md" 'several default methods could be getFirst'
# --server: the supertypes of the game's classes, read from a server's libraries (a stand-in here).
mkdir -p "$TR/game" "$TR/gamey/META-INF" "$TR/server/libraries/net/neoforged/neoforge/21.1.1"
javac --release 21 -d "$TR/game" test/translate/game/net/minecraft/world/Named.java test/translate/game/net/minecraft/world/Light.java test/translate/game/net/minecraft/core/*.java
jar --create --file "$TR/server/libraries/net/neoforged/neoforge/21.1.1/neoforge-21.1.1-server.jar" -C "$TR/game" .
rm -rf "$TR/gamey/com"
javac --release 21 -cp "$TR/game" -d "$TR/gamey" test/translate/gamey/com/example/gamey/Tagged.java
sed 's/numbers/gamey/g; s/Numbers/Gamey/' "$TR/jar/META-INF/neoforge.mods.toml" > "$TR/gamey/META-INF/neoforge.mods.toml"
jar --create --file "$B/jars/gamey.jar" -C "$TR/gamey" .
exits 0 tool translate "$B/jars/gamey.jar" --out "$TR/gamey-out"
says "$OUT" "no --server: the supertypes of the game's classes aren't known"
says "$TR/gamey-out/translation.md" '`com.example.gamey.Tagged.size(I)I`: it makes an object whose supertypes aren'"'"'t all known: com.example.gamey.Tagged$Tag is a net.minecraft.world.Named'
# A method dropped while something it needs may hold, back once what dropped it is gone.
says "$TR/gamey-out/translation.md" '- `com.example.gamey.Tagged.pick(Ljava/lang/String;)I` as `tagged__pick`'
says "$TR/gamey-out/translation.md" '`com.example.gamey.Tagged$Stuck.valueOf(Ljava/lang/String;)Lcom/example/gamey/Tagged$Stuck;`: it initializes com.example.gamey.Tagged$Stuck'
# String.split with a regular expression for separator isn't translated (only the JDK's fast path is modelled).
says "$TR/gamey-out/translation.md" '`com.example.gamey.Tagged.words(Ljava/lang/String;)I`: it splits a text by a regular expression'
# A class of the mod that a virtual call may reach, whose method isn't translated: its makers drop, not the callers.
says "$TR/gamey-out/translation.md" '- `com.example.gamey.Tagged.total(Ljava/util/List;)I` as `tagged__total`'
says "$TR/gamey-out/translation.md" '`com.example.gamey.Tagged.lit()Ljava/lang/Object;`: it makes an object of com.example.gamey.Tagged$Lit that a virtual call may reach, and it calls java.util.function.Consumer.accept'
exits 0 tool translate "$B/jars/gamey.jar" --server "$TR/server" --out "$TR/gamey-out"
says "$OUT" "Read: the supertypes of 4 classes of the game, NeoForge and their libraries, from 1 jars in"
# Each stand-in checked against the server's class: one whose interfaces differ, one that would inherit a method the
# game's declares, are left out.
says "$OUT" "Stand-ins for the game: 24 classes, checked against the game's."
says "$OUT" "- left out: net.minecraft.core.Vec3i: its superclass or interfaces aren't the game's"
says "$OUT" "- left out: net.minecraft.core.BlockPos: the game's class declares getX()I, which the stand-in would take from net.minecraft.core.Vec3i"
says "$TR/gamey-out/translation.md" "- left out: net.minecraft.core.Vec3i"
says "$TR/gamey-out/translation.md" '- `com.example.gamey.Tagged.size(I)I` as `tagged__size`'
says "$TR/gamey-out/translation.md" '`com.example.gamey.Tagged.weight()I`: it makes an object of com.example.gamey.Tagged$Badge that a virtual call may reach, and a virtual call isn'"'"'t certain: com.example.gamey.Tagged$Badge.weight on com.example.gamey.Tagged$Badge it is a default method of net.minecraft.world.Named, outside the jar'
exits 2 tool translate "$B/jars/gamey.jar" --server "$TR/game"
says "$ERR" "isn't a NeoForge server: it has no libraries/ folder"
exits 2 tool translate "$B/jars/fabric.jar"
says "$ERR" '`translate` takes a NeoForge mod'
if command -v rustc > /dev/null; then
  # Debug build: an overflow the translation didn't mean to wrap panics.
  if TRANSLATED_DIR="$PWD/$TR/out" rustc --edition 2021 -o "$TR/check" test/translate/numbers_check.rs > "$TR/rustc.txt" 2>&1; then
    echo "  ok: the translation compiles"
    lacks "$TR/rustc.txt" "warning"
    # HotSpot throws a JVM exception it raised often at one place without its message (OmitStackTraceInFastThrow):
    # the reference is the JVM's semantics, which give one.
    java -XX:-OmitStackTraceInFastThrow -cp "$TR/classes:build/model" NumbersCheck > "$TR/java.txt"
    if "$TR/check" > "$TR/rust.txt" && cmp -s "$TR/java.txt" "$TR/rust.txt"; then
      echo "  ok: Java and the translation give the same $(wc -l < "$TR/java.txt") results"
    else
      echo "  FAIL: Java and the translation differ:"; diff "$TR/java.txt" "$TR/rust.txt" | head -20; failures=$((failures + 1))
    fi
    # The stand-ins against Horizon's own answers (game/README.md): its test of the packed form, an offset that
    # wraps as Java's int does, a position's text and hash (a HashMap's order), north's opposite, normal and name.
    says "$TR/java.txt" "packed limits -> -9223371899415824384"
    says "$TR/java.txt" "eastX 2147483647 -> -2147483648"
    says "$TR/java.txt" "keys 1 -> {BlockPos{x=0, y=-20, z=0}=100}"
    says "$TR/java.txt" "opposite 2 -> 3"
    says "$TR/java.txt" "steps 2 -> -1"
    says "$TR/java.txt" "nameLength 2 -> 5"
  else
    echo "  FAIL: the translation doesn't compile:"; head -30 "$TR/rustc.txt"; failures=$((failures + 1))
  fi
else
  echo "  (skipped: running the translation: there is no rustc here)"
fi

echo; echo "== standins"
# The stand-ins (game/) against a made-up server whose game is a copy of them: the same answers; then a copy whose
# Direction.toString is in upper case: the probe finds it. Nothing of the real game is in the fixture.
SI=$B/standins
for v in same upper; do
  rm -rf "$SI/$v-src" "$SI/$v-classes"
  mkdir -p "$SI/$v-src" "$SI/$v-classes" "$SI/$v/libraries/net/neoforged/neoforge/21.1.1"
  cp -r game/net "$SI/$v-src/"
done
perl -0pi -e 's/(public String toString\(\) \{\n\s*return serializedName)/$1.toUpperCase()/' "$SI/upper-src/net/minecraft/core/Direction.java"
for v in same upper; do
  javac --release 21 -d "$SI/$v-classes" $(find "$SI/$v-src" -name '*.java')
  jar --create --file "$SI/$v/libraries/net/neoforged/neoforge/21.1.1/neoforge-21.1.1-server.jar" -C "$SI/$v-classes" .
done
exits 0 tool standins --server "$SI/same"
says "$OUT" "ok: the 26 stand-ins have the supertypes and the methods of the server's classes"
says "$OUT" "ok: the game and the stand-ins give the same 8956 answers to the probe"
says "$OUT" "no server, no world, no mod"
no_file logs
exits 1 tool standins --server "$SI/upper"
says "$OUT" 'problem: the game gives `direction 0 toString -> DOWN`, the stand-ins `direction 0 toString -> down`'
exits 1 tool standins --server "$SI/upper/libraries/.."
exits 2 tool standins
says "$ERR" '`standins` needs --server'
exits 2 tool standins --server "$SI/same-src"
says "$ERR" "isn't a NeoForge server: it has no libraries/ folder"
exits 2 tool standins "$J" --server "$SI/same"
says "$ERR" '`standins` takes no file, only --server'

echo; echo "== dump"
# Against a stand-in server and a stand-in for Horizon's oracle (test/fixture/horizon-src/tools): nothing starts.
D=$B/dump
exits 2 tool dump "$J" --horizon-src "$H"
says "$ERR" '`dump` needs --server'
exits 2 tool dump "$J" --server "$D/server"
says "$ERR" '`dump` needs --horizon-src'
exits 1 tool dump "$J" --server "$D/nothing" --horizon-src "$H" --out "$D/facts.json"
says "$OUT" "isn't a NeoForge server: there is no libraries/net/neoforged/neoforge/<version>/"
no_file "$D/facts.json"
mkdir -p "$D/server/libraries/net/neoforged/neoforge/21.1.1"
echo '-p libraries/a.jar' > "$D/server/libraries/net/neoforged/neoforge/21.1.1/unix_args.txt"
exits 1 tool dump "$J" --server "$D/server" --horizon-src "$H" --out "$D/facts.json"
says "$OUT" "doesn't say eula=true. Minecraft's EULA is yours to read and accept"
says "$OUT" "this tool never does"
no_file "$D/server/eula.txt"
no_file "$D/facts.json"
echo 'eula=false' > "$D/server/eula.txt"
exits 1 tool dump "$J" --server "$D/server" --horizon-src "$H"
says "$OUT" "doesn't say eula=true"
says "$D/server/eula.txt" 'eula=false'
echo 'eula=true' > "$D/server/eula.txt"
mkdir -p "$D/no-oracle"
exits 1 tool dump "$J" --server "$D/server" --horizon-src "$D/no-oracle"
says "$OUT" "ok: NeoForge 21.1.1, and its EULA is accepted"
says "$OUT" "has no tools/neoforge_oracle.py"
if command -v python3 > /dev/null; then
  exits 0 tool dump "$J" --server "$D/server" --horizon-src "$H" --out "$D/facts.json"
  says "$OUT" "ok: the oracle uses this server for NeoForge 21.1.1"
  says "$OUT" "problem: the machines exporter doesn't compile against this server's jars, so the mod's block entities won't be read"
  says "$OUT" "ok: 1 blocks of the mod's"
  says "$OUT" "port --facts"
  says "$D/facts.json" '"pocketcompass:plain_block"'
  says "$D/facts.json" '"pocketcompass.jar"'
  says "$D/facts.log" "fake oracle, NeoForge 21.1.1"
  if [ -L "$H/.local/neoforge-oracle/21.1.1" ]; then echo "  ok: the oracle's folder for 21.1.1 links to the server"; else echo "  FAIL: no link in $H/.local/neoforge-oracle/"; failures=$((failures + 1)); fi
  exits 1 env FAKE_ORACLE=fail java -Djava.io.tmpdir="$PWD/$B/tmp" -jar build/horizon-port-neoforge.jar dump "$J" --server "$D/server" --horizon-src "$H" --out "$D/failed.json"
  says "$OUT" "problem: the oracle failed (exit 1)"
  says "$OUT" "the mod crashed the server"
  no_file "$D/failed.json"
  mkdir -p "$D/server2/libraries/net/neoforged/neoforge/21.1.1"
  cp "$D/server/libraries/net/neoforged/neoforge/21.1.1/unix_args.txt" "$D/server2/libraries/net/neoforged/neoforge/21.1.1/"
  echo 'eula=true' > "$D/server2/eula.txt"
  exits 1 tool dump "$J" --server "$D/server2" --horizon-src "$H"
  says "$OUT" "already links to another folder"
else
  echo "  (skipped: a dump through the stand-in oracle: there is no python3 here)"
fi
exits 2 tool dump "$B/jars/fabric.jar" --server "$D/server" --horizon-src "$H"
says "$ERR" '`dump` takes a NeoForge mod'
exits 2 tool port "$J" --horizon-src "$H" --out "$P/no-facts" --facts "$D/none.json"
says "$ERR" "which isn't a file"

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
