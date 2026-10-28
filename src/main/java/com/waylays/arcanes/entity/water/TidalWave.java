package com.waylays.arcanes.entity.water;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.water.WaterFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Tidal Wave: a wall of sea rises out of the ground in front of the caster, curling over at its crest, and rolls
 * forwards: every enemy it reaches is swept up and carried along in it, battered; at the end of its run it breaks and
 * crashes down, throwing them all to the ground. Both sides draw it from its age; the server carries.
 */
public class TidalWave extends SpellEffectEntity {
    public static final float WIDTH = 13;
    public static final float HEIGHT = 4.6f;
    public static final float SPEED = .5f;
    public static final int RISE = 10;
    public static final int BREAK = 14;

    private static final EntityDataAccessor<Integer> DATA_RUN = SynchedEntityData.defineId(TidalWave.class, EntityDataSerializers.INT);

    private final IntOpenHashSet carried = new IntOpenHashSet();
    @Nullable
    private String spellId;

    public TidalWave(EntityType<? extends TidalWave> entityType, Level level) {
        super(entityType, level);
    }

    public static TidalWave raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float yaw, int run, float damage) {
        TidalWave wave = new TidalWave(ModEntities.TIDAL_WAVE.get(), level);
        wave.setOwner(caster);
        wave.spellId = spell.getSpellId();
        wave.damage = damage;
        wave.moveTo(at.x, at.y, at.z, yaw, 0);
        wave.entityData.set(DATA_RUN, run);
        level.addFreshEntity(wave);
        WaterFx.sound(level, at, SoundEvents.AMBIENT_UNDERWATER_ENTER, 2, .6f);
        WaterFx.crash(level, at, 2);
        return wave;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RUN, 44);
    }

    /** Ticks it rolls before it breaks. */
    public int getRun() {
        return this.entityData.get(DATA_RUN);
    }

    /** How tall it stands (0 to 1): rising, then falling as it breaks. */
    public float stand(float age) {
        float up = Mth.clamp(age / RISE, 0, 1);
        float down = Mth.clamp((age - getRun()) / BREAK, 0, 1);
        return (1 - (1 - up) * (1 - up)) * (1 - down * down);
    }

    /** How far it has broken (0 to 1). */
    public float broken(float age) {
        return Mth.clamp((age - getRun()) / BREAK, 0, 1);
    }

    public Vec3 forward() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > getRun() + BREAK) {
            discard();
            return;
        }
        Vec3 f = forward();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        if (tickCount == getRun()) {
            // It breaks: everything it carries is thrown down
            for (int id : carried.toIntArray()) {
                Entity e = level.getEntity(id);
                if (e instanceof LivingEntity living && living.isAlive()) {
                    living.setDeltaMovement(f.scale(.5).add(0, -.9, 0));
                    living.hurtMarked = true;
                    if (spell != null) {
                        living.invulnerableTime = 0;
                        DamageSources.applyDamage(living, damage, SpellUtil.damageSource(spell, this, caster));
                    }
                }
            }
            for (int k = -2; k <= 2; k++) {
                SplashCrown.at(level, position().add(f.scale(1.5)).add(side.scale(k * WIDTH / 5)), 2f);
            }
            WaterFx.crash(level, position(), 3);
            SpellUtil.cameraShake(level, position(), 10, 16);
        }
        if (tickCount >= getRun()) {
            return;
        }
        // It rolls on
        Vec3 next = position().add(f.scale(SPEED));
        Vec3 ground = SpellUtil.groundAt(level, next, 3, 2);
        setPos(next.x, Mth.lerp(.4, getY(), ground.y), next.z);
        if (tickCount % 2 == 0) {
            for (int k = -2; k <= 2; k++) {
                WaterFx.spray(level, position().add(f.scale(1)).add(side.scale(k * WIDTH / 5)).add(0, HEIGHT * stand(tickCount), 0), 3, .8);
            }
        }
        if (tickCount % 10 == 0) {
            WaterFx.sound(level, position(), SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS, 2, .8f);
            WaterFx.sound(level, position(), SoundEvents.GENERIC_SPLASH, 1.2f, .5f);
        }
        if (tickCount < RISE / 2) {
            return;
        }
        // Every enemy it reaches is swept up and carried along
        Vec3 c = position();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(WIDTH * .5 + 2, HEIGHT + 2, WIDTH * .5 + 2))) {
            if (!e.isAlive() || e == caster || SpellUtil.isAlly(caster, e)) {
                continue;
            }
            Vec3 d = e.position().subtract(c);
            double along = d.dot(f), across = d.dot(side);
            if (Math.abs(across) > WIDTH * .5 || e.getY() > getY() + HEIGHT + 1) {
                continue;
            }
            boolean held = carried.contains(e.getId());
            if (!held && (along < -1.5 || along > 1.6)) {
                continue;
            }
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1) * .5;
            if (!held) {
                carried.add(e.getId());
                if (spell != null) {
                    e.invulnerableTime = 0;
                    DamageSources.applyDamage(e, damage * .5f, SpellUtil.damageSource(spell, this, caster));
                }
                WaterFx.splash(level, e.getBoundingBox().getCenter(), 14, .4);
            }
            // Carried just in front of the wave's face, tumbling up in it
            Vec3 want = c.add(f.scale(.9)).add(side.scale(across)).add(0, 1.2 + .6 * Mth.sin(tickCount * .3f + e.getId()), 0);
            Vec3 pull = want.subtract(e.position()).scale(.35 * resist);
            e.setDeltaMovement(f.scale(SPEED).add(pull));
            e.hurtMarked = true;
            e.fallDistance = 0;
            WaterFx.soak(e, 40);
            if (!e.canBreatheUnderwater()) {
                e.setAirSupply(Math.max(-20, e.getAirSupply() - 6));
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
