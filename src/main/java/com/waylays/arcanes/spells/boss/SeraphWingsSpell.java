package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossProp;
import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Seraph's Wings (ultimate): two great wings of golden light unfold where the caster stands and gather light while
 * a wide fan of ground ahead of them is marked; then they beat once and a storm of feathers of light blasts across the
 * whole fan, hurling away and lifting all it catches.
 */
@AutoSpellConfig
public class SeraphWingsSpell extends BossSpell {
    public static final float REACH = 16;
    /** Degrees either side of ahead. */
    public static final float HALF_ANGLE = 35;
    public static final float SPAN = 6;

    public SeraphWingsSpell() {
        super("seraph_wings", SpellRarity.LEGENDARY, 3, 60, BossAnimations.SKY_CALL);
        this.manaCostPerLevel = 40;
        this.baseSpellPower = 18;
        this.spellPowerPerLevel = 4;
        this.castTime = 0;
        this.baseManaCost = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.reach", Utils.stringTruncation(REACH, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_POWER_SELECT);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel) {
            Vec3 c = feet(entity);
            float yaw = facing(entity);
            BossTelegraph t = BossTelegraph.cone(level, entity, this, spellLevel, c, yaw, REACH, HALF_ANGLE)
                    .style(BossTelegraph.Style.HOLY).damage(getDamage(spellLevel, entity)).part("wings").reachUp(5).spawn();
            BossProp.scene(level, BossProp.Kind.WINGS, BossTelegraph.Style.HOLY, SPAN, now(level), t.getFireTick(), c, c, c, REACH, yaw);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 1.4f, 1.6f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                knockAway(target, c, 1.5, .5);
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 25, 1));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            }
        }
        Vec3 f = t.forward();
        for (int i = 1; i <= 6; i++) {
            Vec3 p = c.add(f.scale(REACH * i / 6.5));
            SpellUtil.particles(level, ParticleTypes.END_ROD, p.x, p.y + 1, p.z, 6, i * .5, .6, i * .5, .1);
        }
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 4f, .6f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 3f, .5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2f, 1.8f);
        SpellUtil.cameraShake(level, c, 12, REACH + 4);
    }
}
