package com.waylays.arcanes.entity.wall;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wall.WallFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Brick Quake: the caster stamps and the ground answers in three waves. Each runs out in a ring, and as it passes,
 * columns of stone bricks burst up out of the ground, one to three blocks high, leaning out, their mortar glowing,
 * then sink back; whatever the ring passes under is struck and thrown into the air. The last wave is the strongest.
 * Both sides work out the rings from the entity's age.
 */
public class BrickQuake extends SpellEffectEntity {
    public static final int WAVES = 3;
    public static final int FIRST = 8;
    public static final int GAP = 14;
    /** Blocks a ring runs a tick. */
    public static final float SPEED = .55f;
    /** Ticks a column stands, from bursting up to sinking back. */
    public static final int COLUMN_LIFE = 14;

    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(BrickQuake.class, EntityDataSerializers.FLOAT);

    private final IntOpenHashSet[] hit = {new IntOpenHashSet(), new IntOpenHashSet(), new IntOpenHashSet()};
    @Nullable
    private String spellId;

    public BrickQuake(EntityType<? extends BrickQuake> entityType, Level level) {
        super(entityType, level);
    }

    public static BrickQuake stamp(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float radius, float damage) {
        BrickQuake quake = new BrickQuake(ModEntities.BRICK_QUAKE.get(), level);
        quake.setOwner(caster);
        quake.spellId = spell.getSpellId();
        quake.damage = damage;
        quake.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        quake.entityData.set(DATA_RADIUS, radius);
        level.addFreshEntity(quake);
        WallFx.sigil(level, at, 3.5f, FIRST + 10);
        WallFx.sound(level, at, SoundEvents.ANVIL_LAND, 1.2f, .5f);
        return quake;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 10f);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    /** When wave {@code k} sets out. */
    public static int start(int k) {
        return FIRST + k * GAP;
    }

    /** How far out wave {@code k} has run at {@code age}, or a negative number before it sets out. */
    public static float front(int k, float age) {
        return (age - start(k)) * SPEED;
    }

    public int end() {
        return start(WAVES - 1) + (int) (getRadius() / SPEED) + COLUMN_LIFE;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > end()) {
            discard();
            return;
        }
        float radius = getRadius();
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        for (int k = 0; k < WAVES; k++) {
            float r = front(k, tickCount);
            if (tickCount == start(k)) {
                WallFx.slam(level, position(), 1.6f + .4f * k);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, .8f + .3f * k, .5f);
                SpellUtil.cameraShake(level, position(), 10 + 4 * k, radius + 8);
            }
            if (r < 0 || r > radius + 1) {
                continue;
            }
            if (tickCount % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    float a = (i + random.nextFloat()) * Mth.TWO_PI / 10;
                    WallFx.dust(level, position().add(Mth.cos(a) * r, .3, Mth.sin(a) * r), 3, .3);
                }
                WallFx.sound(level, position(), SoundEvents.STONE_BREAK, 1.2f, .5f + .1f * k);
            }
            if (spell == null) {
                continue;
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r + 2, 3, r + 2))) {
                double d = Math.hypot(e.getX() - getX(), e.getZ() - getZ());
                if (!e.isAlive() || SpellUtil.isAlly(caster, e) || d < r - 1.4 || d > r + .5 || e.getY() > getY() + 2.5 || e.getY() < getY() - 2.5
                        || !hit[k].add(e.getId())) {
                    continue;
                }
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage * (k == WAVES - 1 ? 1.5f : 1), SpellUtil.damageSource(spell, this, caster));
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                Vec3 out = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ());
                out = out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize();
                e.setDeltaMovement(e.getDeltaMovement().multiply(.3, 0, .3).add(out.scale(.35 * resist)).add(0, (.55 + .12 * k) * Math.max(.5, resist), 0));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                WallFx.embers(level, e.position().add(0, .3, 0), 6, .3);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
