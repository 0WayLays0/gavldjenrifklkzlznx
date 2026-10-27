package com.waylays.arcanes.entity.bone;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.bone.BoneFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import javax.annotation.Nullable;

/**
 * The Bone Ward: plates of bone (shoulder blades, spined) circle the caster. While any are left the caster takes less
 * damage; a shot that comes near is smashed by a plate, and a blow struck from close by is half turned aside by one,
 * the attacker torn by its splinters. Each plate breaks after one such save. It ends when its time is up or its plates
 * are gone.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class BoneWard extends SpellEffectEntity {
    public static final float RADIUS = 1.35f;
    /** Damage taken while any plate is left, and a blow a plate turns aside. */
    public static final float GUARD = .85f;
    public static final float PARRY = .5f;

    private static final EntityDataAccessor<Integer> DATA_PLATES = SynchedEntityData.defineId(BoneWard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(BoneWard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(BoneWard.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public BoneWard(EntityType<? extends BoneWard> entityType, Level level) {
        super(entityType, level);
    }

    /** Raises the caster's ward (a ward already up is replaced). */
    public static BoneWard raise(Level level, LivingEntity caster, AbstractSpell spell, int plates, int duration, float splinters) {
        BoneWard old = of(level, caster);
        if (old != null) {
            old.discard();
        }
        BoneWard ward = new BoneWard(ModEntities.BONE_WARD.get(), level);
        ward.setOwner(caster);
        ward.spellId = spell.getSpellId();
        ward.damage = splinters;
        ward.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        ward.entityData.set(DATA_PLATES, plates);
        ward.entityData.set(DATA_DURATION, duration);
        ward.entityData.set(DATA_OWNER, caster.getId());
        level.addFreshEntity(ward);
        BoneFx.erupt(level, caster.position(), 1);
        BoneFx.splinters(level, caster.position().add(0, 1, 0), 16, .6);
        return ward;
    }

    /** The ward round {@code caster}, if one is up. */
    @Nullable
    public static BoneWard of(Level level, LivingEntity caster) {
        for (BoneWard ward : level.getEntitiesOfClass(BoneWard.class, caster.getBoundingBox().inflate(4), w -> caster.getUUID().equals(w.getOwnerUUID()))) {
            if (!ward.isRemoved()) {
                return ward;
            }
        }
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PLATES, 6);
        builder.define(DATA_DURATION, 300);
        builder.define(DATA_OWNER, -1);
    }

    public int getPlates() {
        return this.entityData.get(DATA_PLATES);
    }

    public int getDuration() {
        return this.entityData.get(DATA_DURATION);
    }

    /** The warded entity, on both sides. */
    @Nullable
    public Entity ward() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    /** A plate breaks. */
    private void spend(ServerLevel level, Vec3 at) {
        this.entityData.set(DATA_PLATES, Math.max(0, getPlates() - 1));
        BoneFx.splinters(level, at, 14, .3);
        BoneFx.marrow(level, at, 8, .2);
        BoneFx.sound(level, at, SoundEvents.BONE_BLOCK_BREAK, 1.2f, .8f);
        BoneFx.sound(level, at, SoundEvents.SKELETON_HURT, .8f, .6f);
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = ward();
        if (owner != null) {
            setPos(owner.getX(), owner.getY(), owner.getZ());
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity caster = getLivingOwner();
        if (caster == null || !caster.isAlive() || tickCount > getDuration() || getPlates() <= 0) {
            BoneFx.splinters(level, position().add(0, 1, 0), 20, .8);
            discard();
            return;
        }
        // Shots coming near are smashed by a plate
        Vec3 c = caster.getBoundingBox().getCenter();
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, caster.getBoundingBox().inflate(RADIUS + 1.5))) {
            Entity from = shot.getOwner();
            if (shot.isRemoved() || from == caster || (from != null && SpellUtil.isAlly(caster, from)) || shot.position().distanceTo(c) > RADIUS + .9) {
                continue;
            }
            Vec3 at = shot.position();
            shot.discard();
            spend(level, at);
            if (getPlates() <= 0) {
                break;
            }
        }
    }

    /** Less damage while the ward is up; a blow from close by is half turned aside and splinters its striker. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        BoneWard ward = of(level, victim);
        if (ward == null || ward.getPlates() <= 0) {
            return;
        }
        float amount = event.getAmount() * GUARD;
        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof LivingEntity attacker && attacker != victim && attacker.distanceTo(victim) < 4.5f) {
            amount *= PARRY;
            AbstractSpell spell = ward.spellId == null ? null : SpellRegistry.getSpell(ward.spellId);
            if (spell != null) {
                attacker.invulnerableTime = 0;
                DamageSources.applyDamage(attacker, ward.damage, SpellUtil.damageSource(spell, ward, victim));
                double resist = 1 - Mth.clamp(attacker.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                Vec3 away = attacker.position().subtract(victim.position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                attacker.setDeltaMovement(attacker.getDeltaMovement().add(away.scale(.6 * resist)).add(0, .2 * resist, 0));
                attacker.hurtMarked = true;
            }
            ward.spend(level, victim.position().add(attacker.position().subtract(victim.position()).normalize().scale(RADIUS)).add(0, 1, 0));
        }
        event.setAmount(amount);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
