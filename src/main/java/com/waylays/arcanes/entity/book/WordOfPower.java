package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.entity.explosion.BlastEffect;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import com.waylays.arcanes.spells.book.WordOfPowerSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Word of Power: a runic word written in the air in letters of fire, facing the way the caster looked. The letters
 * flare up one after the other, then the whole word explodes forward in a cone of fire. The entity sits at the
 * middle of the word; it keeps the caster's yaw and pitch.
 */
public class WordOfPower extends SpellEffectEntity {
    public static final int LETTERS = 5;
    public static final int LETTER_INTERVAL = 3;
    public static final int DETONATE_TICK = 24;
    public static final int END_TICK = 34;
    public static final float SPACING = 1.15f;
    /** Glyphs in textures/entity/power_glyphs.png (4 x 2). */
    public static final int GLYPHS = 8;

    public WordOfPower(EntityType<? extends WordOfPower> entityType, Level level) {
        super(entityType, level);
    }

    public WordOfPower(Level level, LivingEntity owner, float damage) {
        this(ModEntities.WORD_OF_POWER.get(), level);
        setOwner(owner);
        this.damage = damage;
    }

    /** Which glyph letter {@code i} is: picked from the entity id, so client and server agree. */
    public int glyph(int i) {
        return Math.floorMod(getId() * 31 + i * 5 + i * i, GLYPHS);
    }

    /** Where the caster reads the word: the word's normal, from the caster toward the cone. */
    public Vec3 forward() {
        return Vec3.directionFromRotation(getXRot(), getYRot());
    }

    /** Centre of letter {@code i}, read left to right by the caster. */
    public Vec3 letterPos(int i) {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 left = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        return position().add(left.scale(((LETTERS - 1) * .5f - i) * SPACING));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientParticles();
            return;
        }
        for (int i = 0; i < LETTERS; i++) {
            if (tickCount == 1 + i * LETTER_INTERVAL) {
                Vec3 p = letterPos(i);
                level().playSound(null, p.x, p.y, p.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2f, .7f + i * .09f);
                level().playSound(null, p.x, p.y, p.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.2f + i * .1f);
                SpellUtil.particles(level(), ParticleTypes.FLAME, p.x, p.y, p.z, 10, .25, .4, .25, .03);
                SpellUtil.particles(level(), BookFx.VIOLET_DUST, p.x, p.y, p.z, 6, .3, .45, .3, 0);
            }
        }
        if (tickCount == DETONATE_TICK - 6) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 2f, .5f);
        }
        if (tickCount == DETONATE_TICK) {
            detonate();
        }
        if (tickCount >= END_TICK) {
            discard();
        }
    }

    private void detonate() {
        Entity owner = getOwner();
        Vec3 origin = position();
        Vec3 dir = forward();
        double cos = Math.cos(Math.toRadians(WordOfPowerSpell.CONE_DEGREES));
        AABB box = new AABB(origin, origin).inflate(WordOfPowerSpell.RANGE);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !SpellUtil.isAlly(owner, e))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(origin);
            double dist = to.length();
            // In the cone ahead, or touching the letters themselves
            boolean inCone = dist <= WordOfPowerSpell.RANGE && to.dot(dir) > dist * cos;
            if (!inCone && dist > LETTERS * SPACING * .5 + .8) {
                continue;
            }
            float falloff = (float) Mth.clamp(1.2 - dist / WordOfPowerSpell.RANGE * .5, .6, 1);
            if (DamageSources.applyDamage(e, damage * falloff, SpellUtil.damageSource(ModSpells.WORD_OF_POWER.get(), this, owner))) {
                e.setSecondsOnFire(3);
                Vec3 push = dir.multiply(1, 0, 1).normalize().scale(.9 * falloff);
                e.push(push.x, .35, push.z);
                e.hurtMarked = true;
            }
        }
        for (int i = 0; i < LETTERS; i++) {
            Vec3 p = letterPos(i);
            BlastEffect.spawn(level(), p, .9f, BlastEffect.STYLE_FIRE);
            SpellUtil.particles(level(), ParticleTypes.FLAME, p.x, p.y, p.z, 14, .2, .3, .2, .25);
        }
        // The blast rolls on ahead in a cone of fire
        for (int k = 1; k <= 6; k++) {
            Vec3 p = origin.add(dir.scale(k * WordOfPowerSpell.RANGE / 6.5));
            float spread = k * .35f;
            SpellUtil.particles(level(), ParticleTypes.FLAME, p.x, p.y, p.z, 12, spread, spread * .6, spread, .06);
            SpellUtil.particles(level(), ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 3, spread, spread * .6, spread, .02);
            SpellUtil.particles(level(), BookFx.GILT_DUST, p.x, p.y, p.z, 6, spread, spread * .6, spread, 0);
        }
        Vec3 front = origin.add(dir.scale(1.5));
        SpellUtil.particles(level(), new BlastwaveParticleOptions(new Vector3f(1f, .55f, .15f), 4.5f), front.x, front.y, front.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level(), ParticleTypes.FLASH, origin.x, origin.y, origin.z, 1, 0, 0, 0, 0);
        level().playSound(null, origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.5f, .8f);
        level().playSound(null, origin.x, origin.y, origin.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2f, .6f);
        level().playSound(null, origin.x, origin.y, origin.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 2f, .7f);
        SpellUtil.cameraShake(level(), origin, 10, 14);
    }

    private void clientParticles() {
        if (tickCount >= DETONATE_TICK) {
            return;
        }
        for (int i = 0; i < LETTERS; i++) {
            if (tickCount > 1 + i * LETTER_INTERVAL && random.nextInt(3) == 0) {
                Vec3 p = letterPos(i);
                level().addParticle(ParticleTypes.SMALL_FLAME, p.x + (random.nextFloat() - .5f) * .7, p.y + (random.nextFloat() - .5f) * 1.1, p.z + (random.nextFloat() - .5f) * .7, 0, .03, 0);
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(LETTERS * SPACING, 2, LETTERS * SPACING);
    }
}
