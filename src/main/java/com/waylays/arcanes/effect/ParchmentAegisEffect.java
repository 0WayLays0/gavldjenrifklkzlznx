package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.AegisPages;
import com.waylays.arcanes.entity.book.RazorPage;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import com.waylays.arcanes.spells.book.ParchmentAegisSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Parchment Aegis: pages circle the bearer like armour. Each page stops one enemy projectile flying at the bearer
 * (tridents and fishing hooks are left alone), or takes part of one hit and flies off as a razor page at whoever
 * struck. The pages left are kept on the bearer; the aegis ends with the last one.
 */
@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID)
public class ParchmentAegisEffect extends MobEffect {
    private static final String TAG = ArcanesOublies.MODID + ":parchment_aegis";
    /** Projectiles closer than this to the bearer's centre are caught. */
    public static final double CATCH_RADIUS = 2.6;

    public ParchmentAegisEffect() {
        super(MobEffectCategory.BENEFICIAL, ModSchools.BOOK_COLOR);
    }

    /** Share of a hit a page takes. */
    public static float absorbRatio(int amplifier) {
        return Math.min(.7f, .45f + .05f * amplifier);
    }

    public static void shield(LivingEntity entity, int spellLevel, int duration, int pages, float razorDamage) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Pages", pages);
        tag.putFloat("Damage", razorDamage);
        entity.getPersistentData().put(TAG, tag);
        entity.addEffect(new MobEffectInstance(ModEffects.PARCHMENT_AEGIS.get(), duration, spellLevel - 1, false, false, true));
        AegisPages.attach(entity);
    }

    /** Pages still circling {@code entity} (a full set when the effect was given without the spell). */
    public static int pages(LivingEntity entity) {
        if (!entity.hasEffect(ModEffects.PARCHMENT_AEGIS.get())) {
            return 0;
        }
        CompoundTag tag = entity.getPersistentData().getCompound(TAG);
        return tag.contains("Pages") ? tag.getInt("Pages") : ParchmentAegisSpell.FALLBACK_PAGES;
    }

    private static void usePage(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData().getCompound(TAG);
        tag.putInt("Pages", pages(entity) - 1);
        entity.getPersistentData().put(TAG, tag);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // The pages are not saved with the world: put them back after a reload
        if (!entity.level().isClientSide && AegisPages.of(entity).isEmpty()) {
            AegisPages.attach(entity);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity bearer = event.getEntity();
        MobEffectInstance aegis = bearer.getEffect(ModEffects.PARCHMENT_AEGIS.get());
        Entity attacker = event.getSource().getEntity();
        if (aegis == null || bearer.level().isClientSide || attacker == null || attacker == bearer || pages(bearer) <= 0) {
            return;
        }
        event.setAmount(event.getAmount() * (1 - absorbRatio(aegis.getAmplifier())));
        usePage(bearer);
        Vec3 c = bearer.getBoundingBox().getCenter();
        SpellUtil.particles(bearer.level(), BookFx.paper(), c.x, c.y, c.z, 8, .3, .3, .3, .15);
        bearer.level().playSound(null, c.x, c.y, c.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.5f, 1.3f);
        // The page that took the blow flies back at the attacker
        if (attacker instanceof LivingEntity living && living.isAlive() && living.distanceToSqr(bearer) < 32 * 32 && !SpellUtil.isAlly(bearer, living)) {
            float damage = bearer.getPersistentData().getCompound(TAG).contains("Damage") ? bearer.getPersistentData().getCompound(TAG).getFloat("Damage") : ParchmentAegisSpell.FALLBACK_DAMAGE;
            RazorPage page = new RazorPage(bearer.level(), bearer, damage, 0, ModSpells.PARCHMENT_AEGIS.get());
            Vec3 from = c.add(living.getBoundingBox().getCenter().subtract(c).normalize().scale(.8));
            page.setPos(from.x, from.y, from.z);
            page.shoot(living.getBoundingBox().getCenter().subtract(from), 1.6f);
            bearer.level().addFreshEntity(page);
            bearer.level().playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .8f, 1.7f);
        }
    }

    /** Server side, every tick: catch a projectile flying in; end the aegis once the pages are spent. */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity bearer = event.getEntity();
        if (bearer.level().isClientSide || !bearer.hasEffect(ModEffects.PARCHMENT_AEGIS.get())) {
            return;
        }
        if (pages(bearer) <= 0) {
            bearer.getPersistentData().remove(TAG);
            bearer.removeEffect(ModEffects.PARCHMENT_AEGIS.get());
            return;
        }
        Vec3 c = bearer.getBoundingBox().getCenter();
        for (Projectile projectile : bearer.level().getEntitiesOfClass(Projectile.class, bearer.getBoundingBox().inflate(CATCH_RADIUS))) {
            if (projectile.isRemoved() || projectile instanceof ThrownTrident || projectile instanceof FishingHook
                    || projectile.getOwner() == bearer || SpellUtil.isAlly(bearer, projectile.getOwner() == null ? projectile : projectile.getOwner())
                    || projectile.position().distanceToSqr(c) > CATCH_RADIUS * CATCH_RADIUS
                    || projectile.getDeltaMovement().dot(c.subtract(projectile.position())) <= 0) {
                continue;
            }
            Vec3 p = projectile.position();
            projectile.discard();
            usePage(bearer);
            SpellUtil.particles(bearer.level(), BookFx.paper(), p.x, p.y, p.z, 10, .15, .15, .15, .2);
            SpellUtil.particles(bearer.level(), ParticleTypes.ENCHANT, p.x, p.y, p.z, 14, .1, .1, .1, .8);
            bearer.level().playSound(null, p.x, p.y, p.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 1.5f);
            bearer.level().playSound(null, p.x, p.y, p.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.5f, 1.2f);
            return;
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect() == ModEffects.PARCHMENT_AEGIS.get()) {
            event.getEntity().getPersistentData().remove(TAG);
        }
    }
}
