package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.WallPiece;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Bastion: four walls burst up in a square around the caster, stopping shots and keeping enemies out. */
@AutoSpellConfig
public class BastionSpell extends WallSpell {
    public static final float SIDE = 2.6f;

    public BastionSpell() {
        super("bastion", SpellRarity.RARE, 5, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public static int duration(int spellLevel) {
        return 120 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.stops_shots"),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int life = duration(spellLevel);
        for (int i = 0; i < 4; i++) {
            float yaw = entity.getYRot() + i * 90;
            Vec3 dir = Vec3.directionFromRotation(0, yaw);
            Vec3 at = SpellUtil.groundAt(level, entity.position().add(dir.scale(SIDE)), 3, 1);
            WallPiece.raise(level, entity, this, WallPiece.Kind.STAND, at, yaw, SIDE * 2 + 1, 2.6f, life, 0, 1, 0);
        }
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, life, 0));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
