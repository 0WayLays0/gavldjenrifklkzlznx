#!/usr/bin/env bash
# Looks inside Epic Fight and Custom NPCs (NeoForge 1.21.1), second pass: Epic Fight's data-driven mob patches (folder,
# JSON fields, presets), how other mods register patches and patched renderers, and Custom NPCs' creature and renderer.
# Run by .github/workflows/probe.yml (CI only).
set -u
mkdir -p probe && cd probe
api() { curl -sSL -H 'User-Agent: arcanes-probe' "$1"; }
get() {
  slug=$1
  api "https://api.modrinth.com/v2/project/$slug/version?game_versions=%5B%221.21.1%22%5D&loaders=%5B%22neoforge%22%5D" > $slug.json
  url=$(python3 -c "import json;print(json.load(open('$slug.json'))[0]['files'][0]['url'])")
  curl -sSL -o $slug.jar "$url"; mkdir -p $slug && (cd $slug && unzip -q -o ../$slug.jar)
}
get epic-fight; get customnpcs-unofficial
EF=epic-fight; CN=customnpcs-unofficial
jp() { javap -cp "$1" -p "$2" 2>&1 | head -${3:-120}; }
echo "=== EF nested jars"; find $EF -name '*.jar' | head
echo "=== EF MobPatchReloadListener strings"
for c in $EF/yesman/epicfight/api/data/reloader/MobPatchReloadListener*.class; do echo "--- $c"; strings -n 3 "$c" | grep -vE '^\(|^L.*;$|^\[|^<' | head -120; done
echo "=== EF javap"
for cls in yesman.epicfight.api.data.reloader.MobPatchReloadListener yesman.epicfight.api.data.reloader.MobPatchReloadListener\$CustomHumanoidMobPatchProvider \
  yesman.epicfight.api.data.reloader.MobPatchReloadListener\$AbstractMobPatchProvider yesman.epicfight.world.capabilities.entitypatch.CustomHumanoidMobPatch \
  yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch yesman.epicfight.client.renderer.patched.entity.PCustomHumanoidEntityRenderer \
  yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer yesman.epicfight.api.client.event.types.registry.RegisterPatchedRenderersEvent\$AddEntity \
  yesman.epicfight.api.client.event.types.registry.RegisterPatchedRenderersEvent\$ModifyEntity yesman.epicfight.world.capabilities.provider.CommonEntityPatchProvider \
  yesman.epicfight.world.capabilities.EpicFightCapabilities yesman.epicfight.world.capabilities.item.WeaponCapabilityPresets; do
  echo "--- $cls"; jp $EF "$cls" 90
done
echo "=== EF event classes (registry of entity patches)"; (cd $EF && find . -name '*.class' | grep -iE 'event' | grep -iE 'patch|entity|regist' | head -40)
for c in $(cd $EF && find . -name '*.class' | grep -iE 'EntityPatchRegistryEvent|RegisterEntityPatch|EntityPatchRegist' | head -5); do cls=$(echo $c | sed 's|^\./||; s|\.class$||; s|/|.|g'); echo "--- $cls"; jp $EF "$cls" 60; done
echo "=== EF docs in jar (json examples)"; (cd $EF && find . -iname '*.json' | grep -viE 'lang/|models/|blockstates|recipe|advancement|tags|loot|painting|damage_type|textures|sounds' | head -60)
echo "=== EF built-in mobpatch-like json"; for f in $(cd $EF && grep -rlI --include=*.json -E '"isHumanoid"|"humanoid_weapon_motions"|"combat_behavior"' . | head -3); do echo "--- $f"; head -c 4000 $EF/$f; echo; done
echo "=== CNPC javap"
for cls in noppes.npcs.entity.EntityCustomNpc noppes.npcs.client.renderer.RenderCustomNpc noppes.npcs.client.renderer.RenderNPCInterface noppes.npcs.CustomEntities noppes.npcs.ai.CombatHandler; do
  echo "--- $cls"; jp $CN "$cls" 100
done
echo "=== CNPC ModelData / parts"; (cd $CN && find . -name '*.class' | grep -iE 'ModelData|ModelPart|ModelPartData|ModelScale|PlayerModel|ModelBipedAlt|ModelNpcAlt|ModelPlayerAlt' | head -30)
echo "=== CNPC melee AI"; (cd $CN && find . -name '*.class' | grep -iE 'ai/EntityAI(Attack|Melee|Range|Look|Animation)|CombatHandler' | head -30)
for cls in noppes.npcs.ai.EntityAIAttackTarget noppes.npcs.entity.data.DataAI; do echo "--- $cls"; jp $CN "$cls" 70; done
echo "=== done"
