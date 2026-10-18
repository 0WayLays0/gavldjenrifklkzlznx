package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.BlastEffect;
import com.waylays.arcanes.entity.explosion.ShieldBubble;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.explosion.ExplosionFx;
import com.waylays.arcanes.spells.explosion.ReactiveShieldSpell;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Reactive Shield: a crackling energy shell around the caster. When something hits the caster, the shell takes part
 * of the blow and explodes outward, throwing the attackers back. {@link #CHARGES} explosions, then it is spent.
 */
@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID)
public class ReactiveShieldEffect extends MobEffect {
    public static final int CHARGES = 3;
    /** Hits within this many ticks of an explosion are still softened but do not use up another charge. */
    public static final int REARM_TICKS = 10;
    private static final String TAG = ArcanesOublies.MODID + ":reactive_shield";

    public ReactiveShieldEffect() {
        super(MobEffectCategory.BENEFICIAL, ModSchools.EXPLOSION_COLOR);
    }

    /** Share of each hit the shell takes. */
    public static float absorbRatio(int amplifier) {
        return Math.min(.7f, .35f + .05f * amplifier);
    }

    public static void shield(LivingEntity entity, int spellLevel, int duration, float blastDamage) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Charges", CHARGES);
        tag.putFloat("Damage", blastDamage);
        tag.putLong("LastBlast", -100);
        entity.getPersistentData().put(TAG, tag);
        entity.addEffect(new MobEffectInstance(ModEffects.REACTIVE_SHIELD.get(), duration, spellLevel - 1, false, false, true));
        ShieldBubble.attach(entity);
    }

    public static int charges(LivingEntity entity) {
        if (!entity.hasEffect(ModEffects.REACTIVE_SHIELD.get())) {
            return 0;
        }
        CompoundTag tag = entity.getPersistentData().getCompound(TAG);
        return tag.contains("Charges") ? tag.getInt("Charges") : CHARGES;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 4 == 0;
    }

    /** Crackling sparks across the shell. */
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        // The bubble is not saved with the world: put it back after a reload
        if (entity.tickCount % 20 == 0 && ShieldBubble.bubblesOf(entity).isEmpty()) {
            ShieldBubble.attach(entity);
        }
        float r = entity.getBbHeight() * .65f;
        Vec3 c = entity.getBoundingBox().getCenter();
        float a = entity.getRandom().nextFloat() * Mth.TWO_PI;
        float b = (entity.getRandom().nextFloat() - .5f) * Mth.PI;
        SpellUtil.particles(entity.level(), ExplosionFx.arcaneSparks(), c.x + Mth.cos(a) * Mth.cos(b) * r, c.y + Mth.sin(b) * r, c.z + Mth.sin(a) * Mth.cos(b) * r, 1, 0, 0, 0, .05);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity bearer = event.getEntity();
        MobEffectInstance shield = bearer.getEffect(ModEffects.REACTIVE_SHIELD.get());
        if (shield == null || bearer.level().isClientSide || event.getSource().getEntity() == null || event.getSource().getEntity() == bearer
                || (event.getSource() instanceof SpellDamageSource spellSource && spellSource.spell() == ModSpells.REACTIVE_SHIELD.get())) {
            return;
        }
        event.setAmount(event.getAmount() * (1 - absorbRatio(shield.getAmplifier())));
        CompoundTag tag = bearer.getPersistentData().getCompound(TAG);
        long now = bearer.level().getGameTime();
        if (now - tag.getLong("LastBlast") < REARM_TICKS) {
            return;
        }
        // Given without the spell (/effect): a full shell
        int charges = (tag.contains("Charges") ? tag.getInt("Charges") : CHARGES) - 1;
        tag.putInt("Charges", charges);
        tag.putLong("LastBlast", now);
        bearer.getPersistentData().put(TAG, tag);
        float damage = tag.contains("Damage") ? tag.getFloat("Damage") : ReactiveShieldSpell.FALLBACK_DAMAGE;
        ExplosionFx.detonate(bearer.level(), bearer.getBoundingBox().getCenter(), 1.3f, ReactiveShieldSpell.RADIUS, damage,
                ModSpells.REACTIVE_SHIELD.get(), bearer, bearer, BlastEffect.STYLE_ARCANE);
    }

    /** A spent shell goes away on the bearer's next tick (never while its effects are being ticked). */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity bearer = event.getEntity();
        if (!bearer.level().isClientSide && bearer.hasEffect(ModEffects.REACTIVE_SHIELD.get())
                && bearer.getPersistentData().getCompound(TAG).contains("Charges")
                && bearer.getPersistentData().getCompound(TAG).getInt("Charges") <= 0) {
            bearer.getPersistentData().remove(TAG);
            bearer.removeEffect(ModEffects.REACTIVE_SHIELD.get());
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect() == ModEffects.REACTIVE_SHIELD.get()) {
            event.getEntity().getPersistentData().remove(TAG);
        }
    }
}
