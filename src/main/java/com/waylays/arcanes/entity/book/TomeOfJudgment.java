package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tome of Judgment: a giant book appears high above the target area, opens wide with its pages facing down and
 * comes down over the area; then its two halves swing down and clap shut on the ground, crushing whatever stands
 * between them, and the closed book crumbles into pages. The entity sits on the ground at the centre; the spine
 * runs along its yaw.
 */
public class TomeOfJudgment extends SpellEffectEntity {
    public static final int APPEAR_TICKS = 10;
    public static final int OPEN_END = 24;
    public static final int SLAM_START = 34;
    public static final int IMPACT_TICK = 38;
    public static final int VANISH_TICK = 50;
    public static final int END_TICK = 62;
    /** Book scale against the tome model: page width and height in blocks. */
    public static final float PAGE_WIDTH = 4.6f;
    public static final float PAGE_HEIGHT = 6.2f;
    /** Spine height above the ground once the book is down: the closed halves just touch the ground. */
    public static final float SPINE_HEIGHT = PAGE_WIDTH + .15f;
    public static final float START_HEIGHT = SPINE_HEIGHT + 7f;

    public TomeOfJudgment(EntityType<? extends TomeOfJudgment> entityType, Level level) {
        super(entityType, level);
    }

    public TomeOfJudgment(Level level, LivingEntity owner, float damage) {
        this(ModEntities.TOME_OF_JUDGMENT.get(), level);
        setOwner(owner);
        this.damage = damage;
    }

    /** Height of the spine above the ground at {@code age} ticks. */
    public static float spineHeight(float age) {
        float t = Mth.clamp((age - APPEAR_TICKS) / (OPEN_END - APPEAR_TICKS), 0f, 1f);
        float e = 1 - (1 - t) * (1 - t);
        return Mth.lerp(e, START_HEIGHT, SPINE_HEIGHT + .6f) - Mth.clamp((age - SLAM_START) / (IMPACT_TICK - SLAM_START), 0f, 1f) * .6f;
    }

    /** Spine direction (along the yaw) and the axis the halves swing across. */
    private Vec3 along() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    private Vec3 across() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientParticles();
            return;
        }
        Vec3 c = position();
        if (tickCount == 1) {
            level().playSound(null, c.x, c.y + START_HEIGHT, c.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 4f, .5f);
            level().playSound(null, c.x, c.y + START_HEIGHT, c.z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2f, .5f);
        }
        if (tickCount == APPEAR_TICKS) {
            level().playSound(null, c.x, c.y + START_HEIGHT, c.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 6f, .35f);
        }
        if (tickCount == SLAM_START) {
            level().playSound(null, c.x, c.y + SPINE_HEIGHT, c.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 3f, .7f);
        }
        if (tickCount == IMPACT_TICK) {
            impact();
        }
        if (tickCount == VANISH_TICK) {
            Vec3 m = c.add(0, PAGE_WIDTH * .5, 0);
            SpellUtil.particles(level(), BookFx.paper(), m.x, m.y, m.z, 50, PAGE_HEIGHT * .3, PAGE_WIDTH * .3, PAGE_HEIGHT * .3, .3);
            SpellUtil.particles(level(), ParticleTypes.ENCHANT, m.x, m.y, m.z, 80, PAGE_HEIGHT * .3, PAGE_WIDTH * .3, PAGE_HEIGHT * .3, 1.5);
            level().playSound(null, m.x, m.y, m.z, SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 3f, .5f);
        }
        if (tickCount >= END_TICK) {
            discard();
        }
    }

    /** The halves clap shut: everything between them is crushed and dragged to the middle. */
    private void impact() {
        Entity owner = getOwner();
        Vec3 c = position();
        Vec3 along = along();
        Vec3 across = across();
        double reachAcross = PAGE_WIDTH + .5;
        double reachAlong = PAGE_HEIGHT * .5 + .5;
        AABB box = new AABB(c, c).inflate(Math.max(reachAcross, reachAlong) + 1, 0, Math.max(reachAcross, reachAlong) + 1).expandTowards(0, SPINE_HEIGHT + 1, 0).move(0, -1, 0);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !SpellUtil.isAlly(owner, e))) {
            Vec3 d = e.position().subtract(c);
            double x = d.dot(across);
            double z = d.dot(along);
            if (Math.abs(x) > reachAcross || Math.abs(z) > reachAlong) {
                continue;
            }
            e.invulnerableTime = 0;
            if (DamageSources.applyDamage(e, damage, SpellUtil.damageSource(ModSpells.TOME_OF_JUDGMENT.get(), this, owner))) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
            }
            // Swept in by the closing pages
            Vec3 in = across.scale(-Math.signum(x) * Mth.clamp(Math.abs(x) * .25, .1, .7));
            e.setDeltaMovement(in.x, -.2, in.z);
            e.hurtMarked = true;
        }
        Vec3 m = c.add(0, .3, 0);
        SpellUtil.particles(level(), ParticleTypes.EXPLOSION, m.x, m.y, m.z, 6, along.x * PAGE_HEIGHT * .3, .1, along.z * PAGE_HEIGHT * .3, 0);
        SpellUtil.particles(level(), BookFx.paper(), m.x, m.y + 1, m.z, 40, PAGE_HEIGHT * .25, .8, PAGE_HEIGHT * .25, .35);
        SpellUtil.particles(level(), ParticleTypes.ENCHANT, m.x, m.y + 1, m.z, 80, PAGE_HEIGHT * .25, 1, PAGE_HEIGHT * .25, 2);
        SpellUtil.particles(level(), new BlastwaveParticleOptions(BookFx.VIOLET, PAGE_HEIGHT * 1.3f), m.x, m.y, m.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level(), new BlastwaveParticleOptions(BookFx.GILT, PAGE_HEIGHT * .8f), m.x, m.y, m.z, 1, 0, 0, 0, 0);
        // Gusts out of both open ends of the closing book
        for (int sign = -1; sign <= 1; sign += 2) {
            Vec3 end = m.add(along.scale(sign * PAGE_HEIGHT * .55));
            SpellUtil.particles(level(), ParticleTypes.CLOUD, end.x, end.y, end.z, 16, .4, .3, .4, .25);
        }
        level().playSound(null, c.x, c.y, c.z, SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 6f, .35f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 3f, .45f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.5f, .6f);
        SpellUtil.cameraShake(level(), c, 16, 16);
    }

    private void clientParticles() {
        if (tickCount < APPEAR_TICKS || tickCount > IMPACT_TICK) {
            return;
        }
        // Glyphs raining from the open pages onto the condemned area
        Vec3 c = position();
        float y = spineHeight(tickCount);
        Vec3 along = along();
        Vec3 across = across();
        for (int i = 0; i < 4; i++) {
            Vec3 p = c.add(across.scale((random.nextFloat() * 2 - 1) * PAGE_WIDTH)).add(along.scale((random.nextFloat() - .5f) * PAGE_HEIGHT));
            level().addParticle(ParticleTypes.ENCHANT, p.x, c.y + y - .3, p.z, 0, -1.2, 0);
        }
        if (random.nextInt(2) == 0) {
            Vec3 p = c.add(across.scale((random.nextFloat() * 2 - 1) * PAGE_WIDTH)).add(along.scale((random.nextFloat() - .5f) * PAGE_HEIGHT));
            level().addParticle(BookFx.GILT_DUST, p.x, c.y + .2, p.z, 0, .05, 0);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(PAGE_HEIGHT + 2, START_HEIGHT + 4, PAGE_HEIGHT + 2);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
