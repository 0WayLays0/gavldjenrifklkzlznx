package com.waylays.arcanes.entity.bone;

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

import javax.annotation.Nullable;

/**
 * The Bone Tempest: a whirlwind of bones, three rings of them turning round the caster at different heights and
 * speeds, each bone tumbling: it lacerates every enemy inside it again and again, dragging them round, and smashes the
 * shots that fly into it. At its end the bones fly apart. It follows its caster; both sides work out the bones from
 * its age.
 */
public class BoneTempest extends SpellEffectEntity {
    public static final float RADIUS = 4.3f;
    public static final int GROW = 10;
    public static final int SCATTER = 14;
    public static final int[] COUNT = {12, 15, 18};
    public static final float[] RING = {2.1f, 3.0f, 3.9f};
    public static final float[] HEIGHT = {.5f, 1.3f, 2.1f};
    public static final float[] SPIN = {.13f, -.1f, .08f};

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(BoneTempest.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(BoneTempest.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public BoneTempest(EntityType<? extends BoneTempest> entityType, Level level) {
        super(entityType, level);
    }

    public static BoneTempest raise(Level level, LivingEntity caster, AbstractSpell spell, int life, float damage) {
        for (BoneTempest old : level.getEntitiesOfClass(BoneTempest.class, caster.getBoundingBox().inflate(6), t -> caster.getUUID().equals(t.getOwnerUUID()))) {
            old.discard();
        }
        BoneTempest tempest = new BoneTempest(ModEntities.BONE_TEMPEST.get(), level);
        tempest.setOwner(caster);
        tempest.spellId = spell.getSpellId();
        tempest.damage = damage;
        tempest.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        tempest.entityData.set(DATA_LIFE, life);
        tempest.entityData.set(DATA_OWNER, caster.getId());
        level.addFreshEntity(tempest);
        BoneFx.rattle(level, caster.position(), 2);
        return tempest;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 140);
        builder.define(DATA_OWNER, -1);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    @Nullable
    public Entity body() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    @Override
    public void tick() {
        super.tick();
        Entity body = body();
        if (body != null) {
            setPos(body.getX(), body.getY(), body.getZ());
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int life = getLife();
        if (tickCount > life + SCATTER) {
            discard();
            return;
        }
        LivingEntity caster = getLivingOwner();
        if (caster == null || !caster.isAlive()) {
            discard();
            return;
        }
        if (tickCount >= life) {
            if (tickCount == life) {
                BoneFx.splinters(level, position().add(0, 1.2, 0), 50, 2.5);
                BoneFx.sound(level, position(), SoundEvents.BONE_BLOCK_BREAK, 1.6f, .7f);
            }
            return;
        }
        if (tickCount % 10 == 0) {
            BoneFx.rattle(level, position(), 1.2f);
        }
        // Shots flying into it are smashed
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(RADIUS + 1, 3, RADIUS + 1))) {
            Entity from = shot.getOwner();
            if (shot.isRemoved() || from == caster || (from != null && SpellUtil.isAlly(caster, from))) {
                continue;
            }
            if (Math.hypot(shot.getX() - getX(), shot.getZ() - getZ()) < RADIUS && shot.getY() > getY() - 1 && shot.getY() < getY() + 3.5) {
                BoneFx.splinters(level, shot.position(), 6, .2);
                shot.discard();
            }
        }
        if (tickCount < GROW || tickCount % 8 != 0) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return;
        }
        for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), RADIUS + .5, caster, x -> x.getY() < getY() + 3 && x.getY() > getY() - 1.5)) {
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
            // Dragged round with the whirlwind
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            Vec3 out = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ());
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
            Vec3 round = new Vec3(-out.z, 0, out.x);
            e.setDeltaMovement(e.getDeltaMovement().add(round.scale(.45 * resist)).add(0, .1 * resist, 0));
            e.hurtMarked = true;
            BoneFx.marrow(level, e.getBoundingBox().getCenter(), 4, .3);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
