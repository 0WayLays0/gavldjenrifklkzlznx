package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.entity.fire.FirePillar;
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

/** Fire Pillars: pillars of fire burst up one after another in a line the way the caster looks (see FirePillar). */
@AutoSpellConfig
public class FirePillarsSpell extends StarfireSpell {
    public static final int STAGGER = 4;
    public static final float SPACING = 2.4f;

    public FirePillarsSpell() {
        super("fire_pillars", SpellRarity.UNCOMMON, 8, 10, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    public static int count(int spellLevel) {
        return 4 + spellLevel / 2;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                heatLine(caster),
                Component.translatable("ui.arcanes_oublies.pillars", count(spellLevel), Utils.stringTruncation(hot(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.burns_on", Utils.stringTruncation(hot(spellLevel, caster) * .3f, 1), Utils.timeFromTicks(10, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 f = Vec3.directionFromRotation(0, entity.getYRot());
        int tier = heat(entity);
        float damage = hot(spellLevel, entity);
        for (int k = 0; k < count(spellLevel); k++) {
            Vec3 at = SpellUtil.groundAt(level, entity.position().add(f.scale(2.5 + k * SPACING)), 4, 2);
            FirePillar.raise(level, entity, this, at, k * STAGGER, true, damage, tier);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
