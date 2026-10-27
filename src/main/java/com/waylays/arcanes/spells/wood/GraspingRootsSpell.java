package com.waylays.arcanes.spells.wood;

import com.waylays.arcanes.entity.wood.RootSnare;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.ArrayList;

/**
 * Grasping Roots: roots burst out of the ground round the enemy the caster looks at (or round the enemies where they
 * point) and wind up round it, holding it fast and crushing it (see RootSnare).
 */
@AutoSpellConfig
public class GraspingRootsSpell extends WoodSpell {
    public static final double RANGE = 24;
    public static final double SPOT = 3;

    public GraspingRootsSpell() {
        super("grasping_roots", SpellRarity.UNCOMMON, 8, 10, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    public static int hold(int spellLevel) {
        return 40 + 8 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.rooted", Utils.timeFromTicks(hold(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.crush_per_second", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.cast_range", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        List<LivingEntity> seized = new ArrayList<>();
        LivingEntity aimed = aimed(level, entity, RANGE);
        if (aimed != null) {
            seized.add(aimed);
        } else {
            Vec3 spot = aimedSpot(level, entity, RANGE);
            for (LivingEntity e : SpellUtil.enemiesAround(level, spot.add(0, 1, 0), SPOT, entity, x -> true)) {
                if (seized.size() < 3) {
                    seized.add(e);
                }
            }
            if (seized.isEmpty()) {
                WoodFx.sprout(level, spot, 1);
                WoodFx.chips(level, spot.add(0, .2, 0), 12, .6);
            }
        }
        for (LivingEntity e : seized) {
            RootSnare.seize(level, entity, this, e, hold(spellLevel), getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
