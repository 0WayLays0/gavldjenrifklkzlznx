package com.waylays.arcanes.entity.bone;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.bone.BoneFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The Bone Wyrm: a great serpent of vertebrae, a skull for a head, bursts out of the ground before the caster and
 * hunts: it rises over its prey and dives through it, rebounds and turns on the next, one after another, coiling round
 * its caster when there is nothing to hunt. When its time is up it dives back into the ground. Its body follows the
 * path its head has taken (kept by each client).
 */
public class BoneWyrm extends SpellEffectEntity {
    public static final int EMERGE = 12;
    public static final float SPEED = .85f;
    public static final int SEGMENTS = 22;
    public static final float SPACING = .62f;
    /** Ticks before an enemy it struck can be struck again. */
    public static final int GRACE = 24;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(BoneWyrm.class, EntityDataSerializers.INT);

    private final Int2IntOpenHashMap lastHit = new Int2IntOpenHashMap();
    /** Client side: where the head has been, newest first. */
    public final Deque<Vec3> trail = new ArrayDeque<>();
    private Vec3 velocity = new Vec3(0, .6, 0);
    private int target = -1;
    @Nullable
    private String spellId;

    public BoneWyrm(EntityType<? extends BoneWyrm> entityType, Level level) {
        super(entityType, level);
    }

    public static BoneWyrm summon(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, int life, float damage) {
        BoneWyrm wyrm = new BoneWyrm(ModEntities.BONE_WYRM.get(), level);
        wyrm.setOwner(caster);
        wyrm.spellId = spell.getSpellId();
        wyrm.damage = damage;
        wyrm.moveTo(at.x, at.y - 1.5, at.z, caster.getYRot(), 0);
        wyrm.entityData.set(DATA_LIFE, life);
        Vec3 forward = Vec3.directionFromRotation(0, caster.getYRot());
        wyrm.velocity = forward.scale(.25).add(0, .8, 0).normalize().scale(SPEED);
        level.addFreshEntity(wyrm);
        BoneFx.erupt(level, at, 1.6f);
        BoneFx.sound(level, at, SoundEvents.WITHER_SKELETON_AMBIENT, 1.6f, .5f);
        return wyrm;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 120);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (trail.isEmpty()) {
                // The body still under the ground it is coming out of
                for (int k = 30; k > 0; k--) {
                    trail.addFirst(position().subtract(0, k * .5, 0));
                }
            }
            trail.addFirst(position());
            while (trail.size() > 90) {
                trail.removeLast();
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        LivingEntity caster = getLivingOwner();
        int life = getLife();
        if (tickCount > life + 60 || caster == null) {
            discard();
            return;
        }
        Vec3 pos = position();
        Vec3 aim;
        float speed = SPEED;
        if (tickCount < EMERGE) {
            aim = pos.add(velocity.multiply(1, 0, 1).normalize().scale(.4)).add(0, 1, 0);
            if (tickCount % 3 == 0) {
                BoneFx.splinters(level, pos.add(0, 1.5, 0), 6, .5);
            }
        } else if (tickCount < life) {
            LivingEntity prey = prey(level, caster);
            if (prey != null) {
                Vec3 c = prey.getBoundingBox().getCenter();
                double flat = Math.hypot(c.x - pos.x, c.z - pos.z);
                // Up and over, then a dive through it
                aim = flat > 5 ? c.add(0, 3.5, 0) : c;
                speed = flat > 5 ? SPEED : SPEED * 1.3f;
            } else {
                float a = tickCount * .07f;
                aim = caster.position().add(Mth.cos(a) * 5, 3.5 + Mth.sin(tickCount * .1f), Mth.sin(a) * 5);
            }
        } else {
            // Down into the ground ahead
            aim = pos.add(velocity.multiply(1, 0, 1).scale(3)).subtract(0, 6, 0);
            speed = SPEED * 1.2f;
            if (pos.y < caster.getY() - 3) {
                discard();
                return;
            }
        }
        Vec3 desired = aim.subtract(pos);
        desired = desired.lengthSqr() < 1.0E-4 ? velocity : desired.normalize();
        velocity = velocity.normalize().scale(.78).add(desired.scale(.22)).normalize().scale(speed);
        setPos(pos.add(velocity));
        if (tickCount >= EMERGE && tickCount < life) {
            bite(level, caster);
        }
        if (tickCount % 2 == 0) {
            BoneFx.marrow(level, position(), 2, .2);
        }
        if (tickCount % 12 == 0) {
            BoneFx.rattle(level, position(), 1.2f);
        }
    }

    /** The enemy it hunts: the one it is after if still fair game, else the nearest to its caster it has not just struck. */
    @Nullable
    private LivingEntity prey(ServerLevel level, LivingEntity caster) {
        Entity current = target >= 0 ? level.getEntity(target) : null;
        if (current instanceof LivingEntity living && living.isAlive() && living.distanceTo(caster) < 26 && !struckLately(living)) {
            return living;
        }
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : SpellUtil.enemiesAround(level, caster.position(), 22, caster, x -> !struckLately(x))) {
            double d = e.distanceToSqr(this);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        target = best == null ? -1 : best.getId();
        return best;
    }

    private boolean struckLately(LivingEntity e) {
        return tickCount - lastHit.getOrDefault(e.getId(), -1000) < GRACE;
    }

    private void bite(ServerLevel level, LivingEntity caster) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return;
        }
        for (LivingEntity e : SpellUtil.enemiesAround(level, position(), 1.8, caster, x -> !struckLately(x))) {
            lastHit.put(e.getId(), tickCount);
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            e.setDeltaMovement(e.getDeltaMovement().add(velocity.normalize().scale(.6 * resist)).add(0, .5 * resist, 0));
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            BoneFx.crack(level, e.position(), 1.3f);
            BoneFx.marrow(level, e.getBoundingBox().getCenter(), 12, .4);
            BoneFx.splinters(level, e.getBoundingBox().getCenter(), 8, .4);
            if (e.getId() == target) {
                target = -1;
            }
            // Rebounding up off its prey
            velocity = velocity.add(0, .7, 0);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
