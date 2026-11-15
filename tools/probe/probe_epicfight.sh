#!/usr/bin/env bash
# Fifth pass: the Epic Fight API a bridge needs (getting an entity's patch, playing an animation, knowing Epic Fight's
# own damage, the attack animations by weapon), and how a custom humanoid's renderer is chosen. CI only.
set -u
mkdir -p probe && cd probe
UA='User-Agent: arcanes-probe'
AUTH="Authorization: Bearer ${GH_TOKEN:-}"
api() { curl -sSL -H "$UA" "$1"; }
api "https://api.modrinth.com/v2/project/epic-fight/version?game_versions=%5B%221.21.1%22%5D&loaders=%5B%22neoforge%22%5D" > ef.json
url=$(python3 -c "import json;print(json.load(open('ef.json'))[0]['files'][0]['url'])")
curl -sSL -o ef.jar "$url"; mkdir -p ef && (cd ef && unzip -q -o ../ef.jar)
jp() { echo "--- $1"; javap -cp ef -p "$1" 2>&1 | grep -vE 'lambda\$|access\$' | head -${2:-120}; }
jp yesman.epicfight.world.capabilities.EpicFightCapabilities 60
jp yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch 200
jp yesman.epicfight.world.capabilities.entitypatch.EntityPatch 60
jp yesman.epicfight.api.animation.types.EntityState 60
jp yesman.epicfight.world.damagesource.EpicFightDamageSource 60
jp yesman.epicfight.world.damagesource.EpicFightDamageSources 40
jp yesman.epicfight.api.animation.AnimationManager\$AnimationAccessor 30
jp yesman.epicfight.world.capabilities.item.CapabilityItem 120
jp yesman.epicfight.world.capabilities.item.WeaponCategory 20
jp yesman.epicfight.world.capabilities.item.CapabilityItem\$WeaponCategories 40
echo "=== Animations fields (attack / living)"
javap -cp ef -p yesman.epicfight.gameasset.Animations | grep -E 'public static' | grep -iE 'AUTO|DASH|MOB_|BIPED_(IDLE|WALK|RUN|HOLD|DEATH|HIT|FALL|LAND|KNOCK|SNEAK|SWIM|FLOAT|MOUNT|JUMP)|SPEAR|TACHI|LONGSWORD|DAGGER|GREATSWORD|AXE|FIST|UCHIGATANA|TRIDENT|GUARD|STEP' | head -200
echo "=== animation ids on disk"; (cd ef && find assets/epicfight/animmodels/animations/biped -name '*.json' 2>/dev/null | sed 's|assets/epicfight/animmodels/animations/||' | grep -iE 'combat|living' | head -200)
(cd ef && find . -path '*animations*' -type d | head -20)
echo "=== source: RenderEngine.registerCustomEntityRenderer"
paths=$(curl -sSL -H "$UA" -H "$AUTH" "https://api.github.com/repos/Epic-Fight/epicfight/git/trees/1.21.1?recursive=1" | python3 -c "
import json,sys
d=json.load(sys.stdin)
for x in d.get('tree',[]):
  p=x['path']
  if p.endswith(('client/events/engine/RenderEngine.java','MobCombatBehaviors.java','world/capabilities/entitypatch/HumanoidMobPatch.java','world/capabilities/entitypatch/MobPatch.java')): print(p)")
for p in $paths; do
  echo "--- $p"
  curl -sSL -H "$UA" "https://raw.githubusercontent.com/Epic-Fight/epicfight/1.21.1/$p" > src.java
  case "$p" in
    *RenderEngine.java) grep -n "registerCustomEntityRenderer" -A60 src.java | head -90;;
    *MobCombatBehaviors.java) grep -nE "HUMANOID_(ONEHAND|TWOHAND|SPEAR|GREATSWORD|DAGGER|LONGSWORD|TACHI|UCHIGATANA|FIST|KATANA|AXE)|animationBehavior\(" src.java | head -80;;
    *HumanoidMobPatch.java) grep -n "setWeaponMotions\|initAI\|selectGoalToRemove\|AnimatedAttackGoal\|TargetChasingGoal\|goalSelector" -A8 src.java | head -120;;
    *MobPatch.java) grep -n "initAI\|selectGoalToRemove\|removeGoal\|Goal" -A6 src.java | head -80;;
  esac
done
echo "=== done"
