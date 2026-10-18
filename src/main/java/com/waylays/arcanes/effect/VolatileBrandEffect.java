package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.explosion.ExplosionFx;
import com.waylays.arcanes.spells.explosion.VolatileBrandSpell;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Volatile Brand: a glowing mark burnt into the victim. Every hit it takes (from anyone) adds a charge and makes the
 * mark burn brighter; at {@link #MAX_CHARGES} it explodes. If it runs out first it fizzles harmlessly.
 */
@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID)
public class VolatileBrandEffect extends MobEffect {
    public static final int MAX_CHARGES = 5;
    /** Several hits within this many ticks (multi-hit spells, sweeping) count as one charge. */
    public static final int CHARGE_COOLDOWN = 5;
    private static final String TAG = ArcanesOublies.MODID + ":volatile_brand";

    public VolatileBrandEffect() {
        super(MobEffectCategory.HARMFUL, ModSchools.EXPLOSION_COLOR);
    }

    /** Burns the brand into {@code victim}, remembering the caster and the blast damage (survives a save). */
    public static void brand(LivingEntity victim, LivingEntity caster, int spellLevel, int duration, float damage) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Caster", caster.getUUID());
        tag.putFloat("Damage", damage);
        tag.putInt("Charges", 0);
        tag.putLong("LastCharge", -100);
        victim.getPersistentData().put(TAG, tag);
        victim.addEffect(new MobEffectInstance(ModEffects.VOLATILE_BRAND.get(), duration, spellLevel - 1, false, true, true));
    }

    public static int charges(LivingEntity entity) {
        return entity.getPersistentData().getCompound(TAG).getInt("Charges");
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 2 == 0;
    }

    /** The mark circles the victim's chest, brighter and faster with every charge. */
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        int charges = charges(entity);
        float w = entity.getBbWidth() * .7f;
        double y = entity.getY() + entity.getBbHeight() * .6;
        int points = 2 + charges;
        for (int i = 0; i < points; i++) {
            float angle = entity.tickCount * (.25f + charges * .08f) + i * Mth.TWO_PI / points;
            SpellUtil.particles(entity.level(), charges >= 3 ? ExplosionFx.FIRE_DUST : ExplosionFx.EMBER_DUST,
                    entity.getX() + Mth.cos(angle) * w, y, entity.getZ() + Mth.sin(angle) * w, 1, 0, 0, 0, 0);
        }
        if (charges >= 3 && entity.tickCount % 4 == 0) {
            SpellUtil.particles(entity.level(), ExplosionFx.sparks(), entity.getX(), y, entity.getZ(), charges - 1, w * .4, .2, w * .4, .12);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !victim.hasEffect(ModEffects.VOLATILE_BRAND.get())) {
            return;
        }
        // Only real hits charge it: not burning, poison and other damage nobody dealt, nor the brand's own blast
        if (event.getSource().getEntity() == null
                || (event.getSource() instanceof SpellDamageSource spellSource && spellSource.spell() == ModSpells.VOLATILE_BRAND.get())) {
            return;
        }
        CompoundTag tag = victim.getPersistentData().getCompound(TAG);
        long now = victim.level().getGameTime();
        if (tag.getBoolean("Primed") || now - tag.getLong("LastCharge") < CHARGE_COOLDOWN) {
            return;
        }
        int charges = tag.getInt("Charges") + 1;
        tag.putInt("Charges", charges);
        tag.putLong("LastCharge", now);
        if (charges >= MAX_CHARGES) {
            // Goes off on the victim's next tick, outside this hurt event
            tag.putBoolean("Primed", true);
        }
        victim.getPersistentData().put(TAG, tag);
        Vec3 at = victim.getBoundingBox().getCenter();
        SpellUtil.particles(victim.level(), ExplosionFx.sparks(), at.x, at.y, at.z, 6 + charges * 3, .25, .3, .25, .25 + charges * .05);
        victim.level().playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5f, .7f + charges * .2f);
        ExplosionFx.whine(victim.level(), at, .8f + charges * .25f);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level() instanceof ServerLevel level && victim.hasEffect(ModEffects.VOLATILE_BRAND.get())
                && victim.getPersistentData().getCompound(TAG).getBoolean("Primed")) {
            CompoundTag tag = victim.getPersistentData().getCompound(TAG);
            victim.getPersistentData().remove(TAG);
            victim.removeEffect(ModEffects.VOLATILE_BRAND.get());
            Entity caster = tag.hasUUID("Caster") ? level.getEntity(tag.getUUID("Caster")) : null;
            float damage = tag.contains("Damage") ? tag.getFloat("Damage") : VolatileBrandSpell.FALLBACK_DAMAGE;
            // The hit that primed it just gave the victim hurt immunity; the blast goes through it
            victim.invulnerableTime = 0;
            ExplosionFx.detonate(level, victim.getBoundingBox().getCenter(), 1.8f, VolatileBrandSpell.RADIUS, damage,
                    ModSpells.VOLATILE_BRAND.get(), caster != null ? caster : victim, caster);
        }
    }

    /** Ran out before the fifth hit: the mark fizzles out in a puff of smoke. */
    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        LivingEntity victim = event.getEntity();
        if (instance == null || instance.getEffect() != ModEffects.VOLATILE_BRAND.get() || victim.level().isClientSide) {
            return;
        }
        victim.getPersistentData().remove(TAG);
        Vec3 at = victim.getBoundingBox().getCenter();
        SpellUtil.particles(victim.level(), ParticleTypes.SMOKE, at.x, at.y, at.z, 15, .3, .3, .3, .03);
        victim.level().playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, .8f, 1.2f);
    }
}
