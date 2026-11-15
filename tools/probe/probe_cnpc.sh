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
echo "=== entity types"; grep -rhoE '"(customnpc|npc)[a-z0-9_]*"' noppes/npcs/CustomEntities.java | sort -u | head -60
echo "=== renderer files"; find noppes/npcs/client/renderer noppes/npcs/client/layer -name '*.java' 2>/dev/null | head -80
N=noppes/npcs/entity/EntityNPCInterface.java
grepf $N 'getItemBySlot|setItemSlot|getMainHandItem|getOffhandItem|doHurtTarget|swing\(|ATTACK_DAMAGE|getAttribute|public .*inventory|getHandSlots|getArmorSlots|class EntityNPCInterface' 80
for m in 'ItemStack getItemBySlot' 'boolean doHurtTarget' 'void swing' 'ItemStack getMainHandItem'; do
  ln=$(grep -nF "$m" $N | head -1 | cut -d: -f1); [ -n "$ln" ] && show $N "${ln},$((ln+45))p"
done
show noppes/npcs/entity/EntityCustomNpc.java
show noppes/npcs/entity/data/DataMelee.java 1,200p
for f in noppes/npcs/ModelData.java noppes/npcs/client/model/ModelData.java $(grep -rl "class ModelData" noppes | head -2); do [ -f $f ] && { show $f 1,220p; break; }; done
D=noppes/npcs/entity/data/DataDisplay.java
grepf $D 'skinType|modelType|getSkinTexture|texture|public .*\(' 80
I=noppes/npcs/entity/data/DataInventory.java
grepf $I 'weapons|getRightHand|getLeftHand|getProjectile|armor|public .*\(' 60
show noppes/npcs/client/renderer/RenderCustomNpc.java 1,200p
R=noppes/npcs/client/renderer/RenderNPCInterface.java
grepf $R 'class|getTextureLocation|addLayer|Layer|public .*\(' 60
for f in $(grep -rlE 'class .*Layer' noppes/npcs/client | head -20); do grepf $f 'class |getMainHandItem|getItemBySlot|getRightHand|inventory|renderItem|weapons' 20; done
show noppes/npcs/ai/EntityAIAttackTarget.java 1,200p
echo "=== where swing( is called"; grep -rn "\.swing(" noppes | head -20
echo "=== where getRightHand is synced"; grep -rn "getRightHand\|weapons.put\|weapons.get" noppes | grep -v "^noppes/npcs/client/gui" | head -40
echo "=== done"
