package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gold.GoldFx;
import com.waylays.arcanes.spells.gold.MidasCurseSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Midas Curse: the victim slowly turns to gold. It gets slower and slower until it is frozen as a golden statue,
 * and when the curse runs out the statue shatters. Milk (or any other cure) removes it without the shatter.
 */
@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID)
public class MidasCurseEffect extends MobEffect {
    private static final UUID SPEED_ID = UUID.fromString("5f2c1d3e-7a4b-4c8d-9e1f-a0b1c2d3e4f5");
    private static final UUID ATTACK_SPEED_ID = UUID.fromString("6a3d2e4f-8b5c-4d9e-af20-b1c2d3e4f5a6");
    private static final String TAG = ArcanesOublies.MODID + ":midas_curse";

    public MidasCurseEffect() {
        super(MobEffectCategory.HARMFUL, ModSchools.GOLD_COLOR);
    }

    /** Remembers who cast the curse and how hard the statue shatters (stored on the victim, survives a save). */
    public static void curse(LivingEntity victim, LivingEntity caster, int spellLevel, int duration, float shatterDamage) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Caster", caster.getUUID());
        tag.putFloat("Damage", shatterDamage);
        tag.putInt("Duration", duration);
        victim.getPersistentData().put(TAG, tag);
        victim.addEffect(new MobEffectInstance(ModEffects.MIDAS_CURSE.get(), duration, spellLevel - 1, false, true, true));
    }

    /** 0 when cast, 1 when the statue shatters. */
    public static float progress(LivingEntity entity) {
        MobEffectInstance instance = entity.getEffect(ModEffects.MIDAS_CURSE.get());
        if (instance == null) {
            return 0;
        }
        int total = Math.max(1, entity.getPersistentData().getCompound(TAG).getInt("Duration"));
        return Math.max(0, Math.min(1, 1 - instance.getDuration() / (float) total));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        float progress = progress(entity);
        // Slows to a near standstill well before the end: the last third is the frozen statue
        float slow = Math.min(.97f, progress * 1.5f);
        setModifier(entity, Attributes.MOVEMENT_SPEED, SPEED_ID, -slow);
        setModifier(entity, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, -slow);
        if (progress > .66f) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(0, 1, 0));
        }

        double w = entity.getBbWidth() * .6;
        double h = entity.getBbHeight();
        if (entity.tickCount % 2 == 0) {
            int count = 1 + (int) (progress * 6);
            SpellUtil.particles(entity.level(), GoldFx.GOLD_DUST, entity.getX(), entity.getY() + h * .5, entity.getZ(), count, w, h * .4, w, 0);
        }
        if (progress > .4f && entity.tickCount % 5 == 0) {
            SpellUtil.particles(entity.level(), GoldFx.nuggetShards(), entity.getX(), entity.getY() + h * .7, entity.getZ(), 1 + (int) (progress * 3), w * .6, h * .2, w * .6, .05);
        }
        if (entity.tickCount % 10 == 0) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, .8f + progress, .5f + progress);
        }
    }

    private static void setModifier(LivingEntity entity, Attribute attribute, UUID id, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, "Midas Curse", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap map, int amplifier) {
        super.removeAttributeModifiers(entity, map, amplifier);
        AttributeInstance speed = map.getInstance(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_ID);
        }
        AttributeInstance attackSpeed = map.getInstance(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_ID);
        }
    }

    /** The curse ran its course: the golden statue shatters. */
    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        LivingEntity victim = event.getEntity();
        if (instance == null || instance.getEffect() != ModEffects.MIDAS_CURSE.get() || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag tag = victim.getPersistentData().getCompound(TAG);
        victim.getPersistentData().remove(TAG);
        Entity caster = tag.hasUUID("Caster") ? level.getEntity(tag.getUUID("Caster")) : null;
        float damage = tag.contains("Damage") ? tag.getFloat("Damage") : MidasCurseSpell.FALLBACK_SHATTER_DAMAGE;
        double y = victim.getY() + victim.getBbHeight() * .5;
        GoldFx.shatterBurst(level, victim.getX(), y, victim.getZ(), 1.6f);
        SpellUtil.particles(level, GoldFx.nuggetShards(), victim.getX(), y, victim.getZ(), 30, victim.getBbWidth() * .5, victim.getBbHeight() * .3, victim.getBbWidth() * .5, .35);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.5f, .6f);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, .7f);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, .6f, 1.4f);
        if (caster != null) {
            DamageSources.applyDamage(victim, damage, SpellUtil.damageSource(ModSpells.MIDAS_CURSE.get(), caster, caster));
        } else {
            // Caster gone (logged out, dead, unloaded): a plain magic hit, since a spell source needs an attacker
            victim.hurt(level.damageSources().magic(), damage);
        }
    }
}
