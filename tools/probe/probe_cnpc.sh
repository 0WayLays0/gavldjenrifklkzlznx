#!/usr/bin/env bash
# Decompiles Custom NPCs (NeoForge 1.21.1) to see how an NPC holds its weapon, strikes, and picks its look. CI only.
set -u
mkdir -p probe && cd probe
api() { curl -sSL -H 'User-Agent: arcanes-probe' "$1"; }
api "https://api.modrinth.com/v2/project/customnpcs-unofficial/version?game_versions=%5B%221.21.1%22%5D&loaders=%5B%22neoforge%22%5D" > cn.json
python3 -c "import json;[print(v['version_number']) for v in json.load(open('cn.json'))[:5]]"
url=$(python3 -c "import json;v=[v for v in json.load(open('cn.json')) if v['version_number']=='NeoForge-1.21.1.20241226'] or json.load(open('cn.json'));print(v[0]['files'][0]['url'])")
curl -sSL -o cn.jar "$url"
curl -sSL -o vf.jar https://repo1.maven.org/maven2/org/vineflower/vineflower/1.10.1/vineflower-1.10.1.jar
mkdir -p src && java -jar vf.jar -dgs=1 -rsy=1 cn.jar src > /dev/null 2>&1
mkdir -p s && (cd s && unzip -q -o ../src/cn.jar 2>/dev/null || cp -r ../src/* .)
cd s
show() { echo "=================== $1 (lines ${2:-all})"; if [ -f "$1" ]; then if [ -n "${2:-}" ]; then sed -n "$2" "$1"; else cat "$1"; fi; else echo MISSING; fi; }
grepf() { echo "=================== grep $2 in $1"; grep -nE "$2" "$1" | head -${3:-80}; }
echo "=== lang simple/limited"; for f in $(find assets -name 'en_us*' | head -3); do grep -iE 'simple|limited|basic' $f | head -20; done
show noppes/npcs/client/renderer/RenderCustomNpc.java 200,420p
show noppes/npcs/client/renderer/RenderNPCInterface.java 140,260p
F=$(grep -rl "class ModelDataShared" noppes | head -1); grepf $F 'simpleRender|hasEntity|getEntityName|entityName|public .*\(' 60
echo "=== simpleRender uses"; grep -rn "simpleRender" noppes | head -30
echo "=== client model classes"; find noppes/npcs/client/model -maxdepth 1 -name '*.java' | head -40
echo "=== customnpc renderers registration"; grep -rn "RenderCustomNpc\|registerEntityRenderer\|EntityRenderers.register" noppes --include=*.java | grep -v "^noppes/npcs/client/renderer/RenderCustomNpc" | head -30
echo "=== inventory sync"; grep -rn "inventory.save\|inventory.load\|\"Weapons\"\|getEntityData\|SyncData\|writeSpawnData\|readSpawnData" noppes/npcs/entity/EntityNPCInterface.java | head -20
echo "=== done"
