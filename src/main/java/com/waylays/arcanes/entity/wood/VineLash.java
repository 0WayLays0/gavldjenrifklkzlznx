package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Vine Lash: a thorny vine shoots out of the caster's hand, whipping as it goes.
 * <ul>
 * <li>ENEMY: it winds round the enemy looked at, tears it, and yanks it through the air to the caster's feet.</li>
 * <li>GRAPPLE: it hooks a block looked at with a claw of roots and hauls the caster to it (no fall damage after).</li>
 * <li>MISS: it cracks out to its full length and back, tearing what is in its way.</li>
 * </ul>
 * Both sides draw it from its age and the synced ends; the server pulls.
 */
public class VineLash extends SpellEffectEntity {
    public enum Mode { ENEMY, GRAPPLE, MISS }

    public static final int REACH = 6;
    public static final int PULL = 10;
    public static final int GRAPPLE_MAX = 26;
    public static final int RETRACT = 6;

    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(VineLash.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(VineLash.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(VineLash.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_ANCHOR = SynchedEntityData.defineId(VineLash.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_LET_GO = SynchedEntityData.defineId(VineLash.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public VineLash(EntityType<? extends VineLash> entityType, Level level) {
        super(entityType, level);
    }

    /** {@code target} for ENEMY, else {@code anchor} (the block hooked, or the end of a miss). */
    public static VineLash lash(Level level, LivingEntity caster, AbstractSpell spell, Mode mode, @Nullable LivingEntity target, Vec3 anchor, float damage) {
        VineLash lash = new VineLash(ModEntities.VINE_LASH.get(), level);
        lash.setOwner(caster);
        lash.spellId = spell.getSpellId();
        lash.damage = damage;
        lash.moveTo(caster.getX(), caster.getY(), caster.getZ(), caster.getYRot(), 0);
        lash.entityData.set(DATA_MODE, mode.ordinal());
        lash.entityData.set(DATA_OWNER, caster.getId());
        lash.entityData.set(DATA_TARGET, target == null ? -1 : target.getId());
        lash.entityData.set(DATA_ANCHOR, new Vector3f((float) anchor.x, (float) anchor.y, (float) anchor.z));
        lash.entityData.set(DATA_LET_GO, mode == Mode.GRAPPLE ? GRAPPLE_MAX : REACH + PULL);
        level.addFreshEntity(lash);
        WoodFx.sound(level, caster.position(), SoundEvents.VINE_STEP, 1.6f, .6f);
        WoodFx.sound(level, caster.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1, 1.3f);
        return lash;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODE, 0);
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_ANCHOR, new Vector3f());
        builder.define(DATA_LET_GO, REACH + PULL);
    }

    public Mode getMode() {
        Mode[] all = Mode.values();
        return all[Mth.clamp(this.entityData.get(DATA_MODE), 0, all.length - 1)];
    }

    /** The caster, on both sides. */
    @Nullable
    public Entity lasher() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    @Nullable
    public Entity target() {
        int id = this.entityData.get(DATA_TARGET);
        return id < 0 ? null : level().getEntity(id);
    }

    public Vec3 anchor() {
        Vector3f v = this.entityData.get(DATA_ANCHOR);
        return new Vec3(v.x, v.y, v.z);
    }

    /** The tick the vine lets go and starts back. */
    public int letGo() {
        return this.entityData.get(DATA_LET_GO);
    }

    /** How far out the vine is (0 at the hand, 1 at its end). */
    public float extended(float age) {
        if (age < REACH) {
            float t = age / REACH;
            return 1 - (1 - t) * (1 - t);
        }
        float back = (age - letGo()) / RETRACT;
        return back <= 0 ? 1 : Mth.clamp(1 - back * back, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = lasher();
        if (owner != null) {
            setPos(owner.getX(), owner.getY(), owner.getZ());
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity caster = getLivingOwner();
        if (caster == null || !caster.isAlive() || tickCount > letGo() + RETRACT) {
            discard();
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        switch (getMode()) {
            case ENEMY -> enemy(level, caster, spell);
            case GRAPPLE -> grapple(level, caster);
            case MISS -> miss(level, caster, spell);
        }
    }

    /** It winds round the enemy, tears it, and yanks it to the caster. */
    private void enemy(ServerLevel level, LivingEntity caster, @Nullable AbstractSpell spell) {
        Entity e = target();
        if (!(e instanceof LivingEntity target) || !target.isAlive()) {
            if (tickCount < letGo()) {
                this.entityData.set(DATA_LET_GO, tickCount);
            }
            return;
        }
        if (tickCount == REACH) {
            if (spell != null) {
                target.invulnerableTime = 0;
                DamageSources.applyDamage(target, damage, SpellUtil.damageSource(spell, this, caster));
            }
            WoodFx.leaves(level, target.position().add(0, target.getBbHeight() * .5, 0), 12, .4);
            WoodFx.sound(level, target.position(), SoundEvents.SWEET_BERRY_BUSH_BREAK, 1.4f, .6f);
            WoodFx.sound(level, target.position(), SoundEvents.VINE_BREAK, 1.4f, .5f);
        }
        if (tickCount >= REACH && tickCount < letGo()) {
            // Yanked through the air to the caster's feet
            float yaw = caster.getYRot() * Mth.DEG_TO_RAD;
            Vec3 dest = caster.position().add(-Mth.sin(yaw) * 1.8, .2, Mth.cos(yaw) * 1.8);
            Vec3 to = dest.subtract(target.position());
            double resist = 1 - Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1) * .6;
            Vec3 pull = to.scale(.28);
            if (pull.length() > 1.5) {
                pull = pull.normalize().scale(1.5);
            }
            double lift = tickCount < REACH + 3 ? .32 : 0;
            target.setDeltaMovement(pull.scale(resist).add(0, lift + Math.max(0, to.y) * .1, 0));
            target.hurtMarked = true;
            target.fallDistance = 0;
            if (to.length() < 1.2) {
                this.entityData.set(DATA_LET_GO, tickCount);
            }
        }
        if (tickCount == letGo()) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            WoodFx.sound(level, target.position(), SoundEvents.VINE_STEP, 1.4f, .5f);
        }
    }

    /** It hooks the block and hauls the caster to it. */
    private void grapple(ServerLevel level, LivingEntity caster) {
        Vec3 anchor = anchor();
        if (tickCount == REACH) {
            WoodFx.chips(level, anchor, 14, .3);
            WoodFx.sprout(level, anchor, 1.2f);
        }
        if (tickCount >= REACH && tickCount < letGo()) {
            Vec3 to = anchor.subtract(caster.position().add(0, caster.getBbHeight() * .5, 0));
            if (to.length() < 1.8) {
                this.entityData.set(DATA_LET_GO, tickCount);
            } else {
                Vec3 pull = to.normalize().scale(Math.min(1.45, .5 + to.length() * .12));
                caster.setDeltaMovement(pull.add(0, .06, 0));
                caster.hurtMarked = true;
            }
            caster.fallDistance = 0;
        }
        if (tickCount == letGo()) {
            // A last little hop up and a soft landing
            caster.setDeltaMovement(caster.getDeltaMovement().scale(.5).add(0, .35, 0));
            caster.hurtMarked = true;
            caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, false, false));
            caster.fallDistance = 0;
            WoodFx.sound(level, caster.position(), SoundEvents.VINE_BREAK, 1.2f, .7f);
        }
    }

    /** It cracks out to its full length, tearing what is in its way. */
    private void miss(ServerLevel level, LivingEntity caster, @Nullable AbstractSpell spell) {
        if (tickCount != REACH || spell == null) {
            return;
        }
        Vec3 from = caster.getEyePosition();
        Vec3 to = anchor();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1))) {
            if (!e.isAlive() || e == caster || SpellUtil.isAlly(caster, e)) {
                continue;
            }
            if (e.getBoundingBox().inflate(.6).clip(from, to).isPresent()) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
            }
        }
        WoodFx.sound(level, to, SoundEvents.PLAYER_ATTACK_CRIT, 1.4f, .6f);
        WoodFx.leaves(level, to, 8, .4);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
