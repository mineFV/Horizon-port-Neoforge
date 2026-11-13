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
mkdir -p "$B/classes" "$B/stubs" "$B/mod" "$B/odd" "$B/tidy" "$B/stray" "$B/screen" "$B/jars" "$B/over" "$B/out" "$B/tmp"
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
for t in TomlTest JsonTest MetadataTest LicenceCheckTest ClassDataTest AnalyzerTest ReportTest StandInTest BundleTest EventTableTest WiringTest ContentTest; do
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
