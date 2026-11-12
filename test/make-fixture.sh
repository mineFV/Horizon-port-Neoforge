#!/usr/bin/env bash
# Writes the test fixture into test/fixture/: "Pocket Compass", a made-up NeoForge mod.
# For now it is only the mod's resources (its metadata, a Mixin config, an access transformer, a
# language file and a recipe); its classes come with the milestone that reads them.
# Nothing here comes from a real mod or from the game.
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

F=fixture/pocketcompass
R=$F/resources
rm -rf fixture
mkdir -p "$R/META-INF" "$R/assets/pocketcompass/lang" "$R/data/pocketcompass/recipe"

cat > "$R/META-INF/neoforge.mods.toml" <<'EOF'
# Pocket Compass: the converter's made-up test mod.
modLoader = "javafml"
loaderVersion = "[1,)"
license = "MIT"
issueTrackerURL = "https://example.com/pocketcompass/issues"

[[mods]]
modId = "pocketcompass"
version = "${file.jarVersion}"
displayName = "Pocket Compass"
authors = "Mine, Second Author"
credits = "Thanks to the testers"
logoFile = "pocketcompass.png"
description = '''
Shows which way you face on the HUD.
Made up for the converter's tests.
'''

[[mixins]]
config = "pocketcompass.mixins.json"

[[accessTransformers]]
file = "META-INF/accesstransformer.cfg"

[[dependencies.pocketcompass]]
modId = "neoforge"
type = "required"
versionRange = "[26.3,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.pocketcompass]]
modId = "minecraft"
type = "required"
versionRange = "[26.3,27)"

[[dependencies.pocketcompass]]
modId = "jei"
type = "optional"
versionRange = "*"
side = "CLIENT"
reason = "Shows the compass recipe"
EOF

# The jar's manifest: ${file.jarVersion} in the metadata stands for its Implementation-Version.
cat > "$F/MANIFEST.MF" <<'EOF'
Implementation-Title: Pocket Compass
Implementation-Version: 1.2.3
EOF

cat > "$R/pocketcompass.mixins.json" <<'EOF'
{
  "required": true,
  "package": "com.example.pocketcompass.mixin",
  "compatibilityLevel": "JAVA_21",
  "client": ["MinecraftMixin"]
}
EOF

cat > "$R/META-INF/accesstransformer.cfg" <<'EOF'
# Made up for the test.
public net.minecraft.client.Minecraft someField
EOF

cat > "$R/assets/pocketcompass/lang/en_us.json" <<'EOF'
{ "key.pocketcompass.toggle": "Toggle the compass" }
EOF

cat > "$R/data/pocketcompass/recipe/pocket_compass.json" <<'EOF'
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": ["minecraft:compass"],
  "result": { "id": "pocketcompass:pocket_compass" }
}
EOF

echo "Wrote test/$F"
