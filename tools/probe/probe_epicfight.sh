#!/usr/bin/env bash
# Looks inside Epic Fight and Custom NPCs (NeoForge 1.21.1): their versions, how Epic Fight lets other mods' creatures
# fight with its animations, and what Custom NPCs' creature is. Run by .github/workflows/probe.yml (CI only).
set -u
mkdir -p probe && cd probe
api() { curl -sSL -H 'User-Agent: arcanes-probe' "$1"; }
for slug in epic-fight customnpcs-unofficial; do
  echo "=== $slug versions (NeoForge 1.21.1)"
  api "https://api.modrinth.com/v2/project/$slug/version?game_versions=%5B%221.21.1%22%5D&loaders=%5B%22neoforge%22%5D" > $slug.json
  python3 - "$slug" <<'PY'
import json,sys
slug=sys.argv[1]
d=json.load(open(slug+'.json'))
for v in d[:8]:
    print(v['version_number'], v['id'], v['date_published'][:10], v['files'][0]['filename'])
v=d[0]
open(slug+'.url','w').write(v['files'][0]['url'])
open(slug+'.ver','w').write(v['version_number'])
PY
  curl -sSL -o $slug.jar "$(cat $slug.url)"
  ls -la $slug.jar
  mkdir -p $slug && (cd $slug && unzip -q -o ../$slug.jar)
done
echo "=== Epic Fight: mods.toml"; cat epic-fight/META-INF/neoforge.mods.toml 2>/dev/null | head -40
echo "=== Epic Fight: packages"; (cd epic-fight && find . -name '*.class' | sed 's|/[^/]*$||' | sort -u | head -150)
echo "=== Epic Fight: classes about patches, mob patch data, events"
(cd epic-fight && find . -name '*.class' | grep -iE 'MobPatch|EntityPatch|PatchProvider|Humanoid|RegistryEvent|event/|CustomHumanoid|MobCombatBehavior|PatchedRenderers|PRenderer|AnimationManager|Animations\.class|WeaponCapability|Style' | head -200)
echo "=== Epic Fight: data folders"; (cd epic-fight && find data -maxdepth 3 -type d | head -60)
echo "=== Epic Fight: example mob patch files"; (cd epic-fight && find . -path '*mobpatch*' -o -path '*mob_patch*' | head -30)
for f in $(cd epic-fight && find . -path '*mobpatch*' -name '*.json' | head -3); do echo "--- $f"; head -c 3000 epic-fight/$f; echo; done
echo "=== Epic Fight: javap of key classes"
CP=epic-fight
for c in $(cd epic-fight && find . -name '*.class' | grep -E '/(EntityPatchRegistryEvent|PatchedRenderersEvent|MobPatchReloadListener|CustomHumanoidMobPatch|HumanoidMobPatch|MobPatch|LivingEntityPatch|EntityPatchProvider|CombatBehaviors|HumanoidMobPatch\$|EpicFightCapabilities)\.class$' | head -20); do
  cls=$(echo $c | sed 's|^\./||; s|\.class$||; s|/|.|g'); echo "--- $cls"; javap -cp $CP -p "$cls" 2>&1 | head -80
done
echo "=== Epic Fight: strings mentioning mobpatch"
grep -rl --include=*.class -a 'epicfight_mobpatch\|mobpatch' epic-fight | head -10
for c in $(grep -rl --include=*.class -a 'mobpatch' epic-fight | head -4); do echo "--- $c"; strings -n 6 $c | grep -iE 'mobpatch|json|model|armature|renderer|humanoid|faction|combat|behavior|weapon|attack|preset|isHumanoid|default' | head -60; done
echo "=== Custom NPCs: mods.toml"; cat customnpcs-unofficial/META-INF/neoforge.mods.toml 2>/dev/null | head -40
echo "=== Custom NPCs: entity classes"; (cd customnpcs-unofficial && find . -name '*.class' | grep -iE 'entity/Entity(Custom)?Npc|EntityNPCInterface|EntityCustomNpc|registry|CustomEntities|ModelNpc|RenderNpc|RenderCustomNpc' | head -60)
for c in $(cd customnpcs-unofficial && find . -name '*.class' | grep -E '/(EntityNPCInterface|EntityCustomNpc|CustomEntities)\.class$' | head -4); do
  cls=$(echo $c | sed 's|^\./||; s|\.class$||; s|/|.|g'); echo "--- $cls"; javap -cp customnpcs-unofficial -p "$cls" 2>&1 | head -160
done
echo "=== Custom NPCs: entity type ids"; grep -rhao --include=*.class 'customnpc[a-z_]*' customnpcs-unofficial | sort | uniq -c | sort -rn | head -20
echo "=== done"
