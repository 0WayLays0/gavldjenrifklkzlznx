package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gem.Gem;
import com.waylays.arcanes.spells.gem.GemFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * The Lapis Constellation: seven stars of lapis (the night sky, flecked with gold) circle the caster on a tilted ring,
 * joined by threads of golden light like a constellation. When an enemy comes near, a star leaves the ring and darts
 * at it; the others close up the figure.
 */
public class LapisConstellation extends SpellEffectEntity {
    public static final int STARS = 7;
    public static final float RADIUS = 2.4f;
    public static final float RANGE = 8;
    public static final int LAUNCH_TICKS = 8;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(LapisConstellation.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STARS = SynchedEntityData.defineId(LapisConstellation.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(LapisConstellation.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public LapisConstellation(EntityType<? extends LapisConstellation> entityType, Level level) {
        super(entityType, level);
    }

    /** The caster's constellation (a new one replaces the old). */
    public static LapisConstellation gather(Level level, LivingEntity caster, AbstractSpell spell, int life, float damage) {
        for (LapisConstellation old : level.getEntitiesOfClass(LapisConstellation.class, caster.getBoundingBox().inflate(8),
                c -> caster.getUUID().equals(c.getOwnerUUID()))) {
            old.discard();
        }
        LapisConstellation sky = new LapisConstellation(ModEntities.LAPIS_CONSTELLATION.get(), level);
        sky.setOwner(caster);
        sky.spellId = spell.getSpellId();
        sky.damage = damage;
        sky.moveTo(caster.getX(), caster.getY(), caster.getZ(), level.random.nextFloat() * 360, 0);
        sky.entityData.set(DATA_OWNER, caster.getId());
        sky.entityData.set(DATA_STARS, (1 << STARS) - 1);
        sky.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(sky);
        GemFx.sound(level, caster.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1.4f, 1.2f);
        GemFx.chime(level, caster.position(), 1f, 1.8f);
        return sky;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_STARS, (1 << STARS) - 1);
        builder.define(DATA_LIFE, 400);
    }

    @Nullable
    public Entity getCaster() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    /** Which stars are still on the ring (bit {@code i} for star {@code i}). */
    public int getStars() {
        return this.entityData.get(DATA_STARS);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /**
     * Where star {@code i} is from the caster's feet at age {@code age}: on a ring that turns and slowly tilts, each
     * star bobbing on its own.
     */
    public Vec3 star(int i, float age) {
        float a = getYRot() * Mth.DEG_TO_RAD + i * Mth.TWO_PI / STARS + age * .045f;
        float tilt = .35f * Mth.sin(age * .02f);
        float x = Mth.cos(a) * RADIUS;
        float z = Mth.sin(a) * RADIUS;
        float y = 1.3f + z * tilt + .15f * Mth.sin(age * .12f + i * 1.7f);
        return new Vec3(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity owner = getLivingOwner();
        int stars = getStars();
        if (owner == null || !owner.isAlive() || tickCount > getLife() || stars == 0) {
            if (owner != null && stars != 0) {
                GemFx.glitter(level, owner.position().add(0, 1.3, 0), Gem.LAPIS, 20, RADIUS * .6);
            }
            discard();
            return;
        }
        setPos(owner.getX(), owner.getY(), owner.getZ());
        if (tickCount % LAUNCH_TICKS != 0 || tickCount < 10) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        List<LivingEntity> foes = SpellUtil.enemiesAround(level, owner.position().add(0, 1, 0), RANGE, owner, e -> owner.hasLineOfSight(e));
        if (foes.isEmpty() || spell == null) {
            return;
        }
        foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(owner)));
        // The star nearest the enemy leaves the ring
        LivingEntity foe = foes.get(0);
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < STARS; i++) {
            if ((stars & (1 << i)) != 0) {
                double d = position().add(star(i, tickCount)).distanceToSqr(foe.position());
                if (d < bestDistance) {
                    bestDistance = d;
                    best = i;
                }
            }
        }
        if (best >= 0) {
            this.entityData.set(DATA_STARS, stars & ~(1 << best));
            GemShard.fling(level, owner, spell, Gem.LAPIS, position().add(star(best, tickCount)), foe, damage);
            GemFx.sound(level, position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.5f + .1f * Integer.bitCount(stars));
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
