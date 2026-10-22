package com.waylays.arcanes.spells.verdigris;

import com.waylays.arcanes.entity.verdigris.AncientPillar;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.util.Mth;
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

/** Colonnade: a ring of ancient columns bursts out of the ground around the caster, throwing every enemy near away. */
@AutoSpellConfig
public class ColonnadeSpell extends VerdigrisSpell {
    public static final int COLUMNS = 8;
    public static final float RING = 2.6f;

    public ColonnadeSpell() {
        super("colonnade", SpellRarity.RARE, 6, 16, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RING + 1.8, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float damage = getPower(spellLevel, entity);
        for (int i = 0; i < COLUMNS; i++) {
            float a = i * Mth.TWO_PI / COLUMNS + entity.getYRot() * Mth.DEG_TO_RAD;
            Vec3 at = SpellUtil.groundAt(level, entity.position().add(Mth.cos(a) * RING, 0, Mth.sin(a) * RING), 3, 1);
            AncientPillar.raise(level, entity, this, AncientPillar.Kind.COLUMN, at, 1 + i % 2, 60, .9f, damage);
        }
        SpellUtil.cameraShake(level, entity.position(), 10, 10);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
