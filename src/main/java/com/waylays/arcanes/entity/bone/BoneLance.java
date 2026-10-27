package com.waylays.arcanes.entity.bone;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.bone.BoneFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A flying bone of Bone Magic, its marrow glowing:
 * <ul>
 * <li>LANCE: a long bone spear, sharpened to a point, that runs through up to {@link #PIERCE} enemies, leaving them
 * bleeding marrow (slowed), and splinters on the ground.</li>
 * <li>SHARD: a splinter of bone, one of a volley; it stops in the first enemy.</li>
 * </ul>
 */
public class BoneLance extends SpellProjectile {
    public enum Kind { LANCE, SHARD }

    public static final int PIERCE = 3;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(BoneLance.class, EntityDataSerializers.INT);

    private final IntOpenHashSet hit = new IntOpenHashSet();
    @Nullable
    private String spellId;

    public BoneLance(EntityType<? extends BoneLance> entityType, Level level) {
        super(entityType, level);
    }

    public static BoneLance throwBone(Level level, LivingEntity owner, AbstractSpell spell, Kind kind, Vec3 from, Vec3 velocity, float damage) {
        BoneLance bone = new BoneLance(ModEntities.BONE_LANCE.get(), level);
        bone.setOwner(owner);
        bone.spellId = spell.getSpellId();
        bone.entityData.set(DATA_KIND, kind.ordinal());
        bone.setDamage(damage);
        bone.setPos(from.x, from.y - bone.getBbHeight() * .5, from.z);
        bone.setDeltaMovement(velocity);
        bone.setYRot((float) (Mth.atan2(velocity.x, velocity.z) * Mth.RAD_TO_DEG));
        bone.setXRot((float) (Mth.atan2(velocity.y, velocity.horizontalDistance()) * Mth.RAD_TO_DEG));
        bone.yRotO = bone.getYRot();
        bone.xRotO = bone.getXRot();
        level.addFreshEntity(bone);
        return bone;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    @Override
    protected double gravity() {
        return getKind() == Kind.LANCE ? .012 : .025;
    }

    @Override
    protected int lifetime() {
        return 60;
    }

    @Override
    protected boolean piercesEntities() {
        return getKind() == Kind.LANCE;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !SpellUtil.isAlly(getOwner(), target) && !hit.contains(target.getId());
    }

    @Override
    protected void onImpact(HitResult hit) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = hit.getLocation();
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity struck) {
            this.hit.add(struck.getId());
            if (spell != null) {
                struck.invulnerableTime = 0;
                DamageSources.applyDamage(struck, damage, SpellUtil.damageSource(spell, this, getOwner()));
                double resist = 1 - Mth.clamp(struck.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                Vec3 push = getDeltaMovement().normalize();
                struck.setDeltaMovement(struck.getDeltaMovement().add(push.scale((getKind() == Kind.LANCE ? .5 : .2) * resist)));
                struck.hurtMarked = true;
                if (getKind() == Kind.LANCE) {
                    struck.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
                }
            }
            BoneFx.marrow(level, struck.getBoundingBox().getCenter(), 10, .3);
            BoneFx.splinters(level, at, getKind() == Kind.LANCE ? 10 : 4, .25);
            BoneFx.crack(level, at, getKind() == Kind.LANCE ? 1f : .6f);
            if (getKind() == Kind.LANCE && this.hit.size() < PIERCE) {
                // On through: a little slower
                setDeltaMovement(getDeltaMovement().scale(.85));
                return;
            }
        } else {
            BoneFx.splinters(level, at, getKind() == Kind.LANCE ? 16 : 5, .3);
            BoneFx.sound(level, at, net.minecraft.sounds.SoundEvents.BONE_BLOCK_BREAK, getKind() == Kind.LANCE ? 1.2f : .6f, 1);
        }
        discard();
    }

    @Override
    protected void clientTick() {
        Vec3 p = position().add(0, getBbHeight() * .5, 0);
        level().addParticle(BoneFx.MARROW_DUST, p.x, p.y, p.z, 0, 0, 0);
    }
}
