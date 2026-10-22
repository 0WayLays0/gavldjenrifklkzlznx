package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gold.GoldCoat;
import com.waylays.arcanes.spells.gold.GoldFx;
import com.waylays.arcanes.spells.gold.MidasCurseSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.UUID;

/**
 * Midas Curse: the victim turns to gold from the feet up (drawn on its own body, see {@link GoldCoat}). It gets slower
 * and slower until it is frozen as a golden statue, and when the curse runs out the statue shatters in coins and
 * shards that gild everything around it. A victim already gilded through is a statue at once. Milk (or any other cure)
 * removes it without the shatter.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class MidasCurseEffect extends MobEffect {
    private static final ResourceLocation SPEED_ID = ArcanesOublies.id("effect.midas_curse.movement_speed");
    private static final ResourceLocation ATTACK_SPEED_ID = ArcanesOublies.id("effect.midas_curse.attack_speed");
    private static final String TAG = ArcanesOublies.MODID + ":midas_curse";

    public MidasCurseEffect() {
        super(MobEffectCategory.HARMFUL, ModSchools.GOLD_COLOR);
    }

    /** The part of the curse spent turning to gold: after it, the victim is a frozen statue until it shatters. */
    public static final float TURNED = .66f;

    /**
     * Remembers who cast the curse and how hard the statue shatters (stored on the victim, survives a save). A victim
     * already gilded through (five coats of Gilding) is a statue at once.
     */
    public static void curse(LivingEntity victim, LivingEntity caster, int spellLevel, int duration, float shatterDamage) {
        boolean gilded = GildedEffect.coats(victim) >= GildedEffect.MAX_COATS;
        int left = gilded ? Math.max(20, (int) (duration * (1 - TURNED))) : duration;
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Caster", caster.getUUID());
        tag.putFloat("Damage", shatterDamage);
        tag.putInt("Duration", duration);
        tag.putLong("Start", victim.level().getGameTime() - (duration - left));
        victim.getPersistentData().put(TAG, tag);
        victim.addEffect(new MobEffectInstance(ModEffects.MIDAS_CURSE, left, spellLevel - 1, false, true, true));
        GoldCoat.syncLater(victim);
    }

    /** The game time the curse began (as if from the start, for a victim cursed already gilded through). */
    public static long start(LivingEntity entity) {
        return entity.getPersistentData().getCompound(TAG).getLong("Start");
    }

    /** The curse's whole length, in ticks. */
    public static int total(LivingEntity entity) {
        return Math.max(1, entity.getPersistentData().getCompound(TAG).getInt("Duration"));
    }

    /** 0 when cast, 1 when the statue shatters. */
    public static float progress(LivingEntity entity) {
        MobEffectInstance instance = entity.getEffect(ModEffects.MIDAS_CURSE);
        if (instance == null) {
            return 0;
        }
        int total = Math.max(1, entity.getPersistentData().getCompound(TAG).getInt("Duration"));
        return Math.max(0, Math.min(1, 1 - instance.getDuration() / (float) total));
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return true;
        }
        float progress = progress(entity);
        // Slows to a near standstill well before the end: the last third is the frozen statue
        float slow = Math.min(.97f, progress / TURNED * .97f);
        setModifier(entity, Attributes.MOVEMENT_SPEED, SPEED_ID, -slow);
        setModifier(entity, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, -slow);
        if (progress > TURNED) {
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
        return true;
    }

    private static void setModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @Override
    public void removeAttributeModifiers(AttributeMap map) {
        super.removeAttributeModifiers(map);
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
        if (instance == null || !instance.getEffect().is(ModEffects.MIDAS_CURSE.getKey()) || !(victim.level() instanceof ServerLevel level)) {
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
        // The statue's shards gild everything around it
        for (LivingEntity near : SpellUtil.enemiesAround(level, victim.position().add(0, victim.getBbHeight() * .5, 0), 4, caster, e -> e != victim)) {
            GoldCoat.gild(near, 2);
            if (caster != null) {
                DamageSources.applyDamage(near, damage * .4f, SpellUtil.damageSource(ModSpells.MIDAS_CURSE.get(), caster, caster));
            }
        }
        GoldFx.coins(level, victim.position().add(0, victim.getBbHeight() * .5, 0), 18, .45f);
        if (caster != null) {
            DamageSources.applyDamage(victim, damage, SpellUtil.damageSource(ModSpells.MIDAS_CURSE.get(), caster, caster));
        } else {
            // Caster gone (logged out, dead, unloaded): a plain magic hit, since a spell source needs an attacker
            victim.hurt(level.damageSources().magic(), damage);
        }
    }
}
