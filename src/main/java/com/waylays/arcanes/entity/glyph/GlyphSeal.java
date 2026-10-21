package com.waylays.arcanes.entity.glyph;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import com.waylays.arcanes.spells.glyph.GlyphFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * Elemental Seal: the six element glyphs, written one after another in a ring around the caster, turn with them. At
 * a steady beat the next glyph in the ring looses its bolt at the nearest enemy, so the six elements follow each other
 * on whatever comes close (and set off their reactions).
 */
public class GlyphSeal extends SpellEffectEntity {
    public static final int APPEAR_TICKS = 12;
    public static final int FADE_TICKS = 10;
    public static final float RANGE = 14;
    public static final float ORBIT = 1.35f;
    public static final float HEIGHT = 1.1f;
    /** Degrees the ring turns each tick. */
    public static final float SPIN = 3;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(GlyphSeal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(GlyphSeal.class, EntityDataSerializers.INT);
    /** The glyph that fires next, and when the last one fired. */
    private static final EntityDataAccessor<Integer> DATA_NEXT = SynchedEntityData.defineId(GlyphSeal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHOT_TICK = SynchedEntityData.defineId(GlyphSeal.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private int shotTicks = 20;
    private int nextShot = APPEAR_TICKS;

    public GlyphSeal(EntityType<? extends GlyphSeal> entityType, Level level) {
        super(entityType, level);
    }

    /** Raises a seal around {@code caster}, replacing the one it already had. */
    public static GlyphSeal raise(Level level, LivingEntity caster, AbstractSpell spell, int life, float damage, int shotTicks) {
        for (GlyphSeal old : level.getEntitiesOfClass(GlyphSeal.class, caster.getBoundingBox().inflate(4), s -> caster.getUUID().equals(s.getOwnerUUID()))) {
            old.discard();
        }
        GlyphSeal seal = new GlyphSeal(ModEntities.GLYPH_SEAL.get(), level);
        seal.setOwner(caster);
        seal.spellId = spell.getSpellId();
        seal.damage = damage;
        seal.shotTicks = shotTicks;
        seal.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        seal.entityData.set(DATA_OWNER, caster.getId());
        seal.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(seal);
        return seal;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_LIFE, 200);
        builder.define(DATA_NEXT, 0);
        builder.define(DATA_SHOT_TICK, -1000);
    }

    /** The caster, on both sides. */
    @Nullable
    public Entity getBearer() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public int getNext() {
        return this.entityData.get(DATA_NEXT);
    }

    public float sinceShot(float partialTicks) {
        return tickCount + partialTicks - this.entityData.get(DATA_SHOT_TICK);
    }

    /** Degrees around the caster of glyph {@code i}. */
    public float angle(int i, float partialTicks) {
        return (tickCount + partialTicks) * SPIN + i * 60;
    }

    /** How far glyph {@code i} is written (they appear one after another). */
    public float written(int i, float partialTicks) {
        return Mth.clamp((tickCount + partialTicks - i * 2f) / 4f, 0, 1);
    }

    /** 1 while it holds, fading at the end. */
    public float strength(float partialTicks) {
        return Mth.clamp(1 - (tickCount + partialTicks - getLife()) / FADE_TICKS, 0, 1);
    }

    /** Where glyph {@code i} is, relative to the caster's feet. */
    public Vec3 offset(int i, float partialTicks) {
        float a = angle(i, partialTicks) * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(a) * ORBIT, HEIGHT + .12 * Mth.sin((tickCount + partialTicks) * .15f + i), Mth.sin(a) * ORBIT);
    }

    @Override
    public void tick() {
        super.tick();
        Entity bearer = getBearer();
        if (bearer != null) {
            setPos(bearer.getX(), bearer.getY(), bearer.getZ());
        }
        if (level().isClientSide) {
            return;
        }
        if (!(getOwner() instanceof LivingEntity caster) || !caster.isAlive() || tickCount > getLife() + FADE_TICKS) {
            discard();
            return;
        }
        if (tickCount < nextShot || tickCount >= getLife()) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity target = SpellUtil.enemiesAround(level(), caster.position(), RANGE, caster, e -> !(e instanceof ArmorStand) && caster.hasLineOfSight(e))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(caster))).orElse(null);
        if (spell == null || target == null) {
            return;
        }
        int i = getNext();
        GlyphElement element = GlyphElement.byId(i);
        Vec3 from = caster.position().add(offset(i, 0));
        Vec3 to = target.position().add(0, target.getBbHeight() * .5, 0);
        GlyphBolt bolt = GlyphBolt.shoot(level(), caster, spell, element, from, to.subtract(from), 1.4f, damage);
        if (element == GlyphElement.STONE) {
            bolt.withSplash(1.5f);
        }
        GlyphFx.burst(level(), element, from, 6, .15);
        GlyphFx.writing(level(), element, from);
        this.entityData.set(DATA_NEXT, (i + 1) % GlyphElement.values().length);
        this.entityData.set(DATA_SHOT_TICK, tickCount);
        nextShot = tickCount + shotTicks;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64 * 64;
    }
}
