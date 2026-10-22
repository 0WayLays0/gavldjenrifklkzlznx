package com.waylays.arcanes.entity.verdigris;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.verdigris.VerdigrisFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
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

/**
 * Sunken Temple (ultimate): the ruins of an ancient temple rise around the target: a ring of tall columns of
 * verdigrised copper, mossy and hung with vines, joined by stone lintels. Those inside are shut in (pushed back from the
 * edge) while rubble rains on them; then the columns topple inward, all at once, burying everything inside.
 */
public class SunkenTemple extends SpellEffectEntity {
    public static final int COLUMNS = 8;
    public static final float RADIUS = 6;
    public static final float COLUMN_HEIGHT = 6;
    public static final int STAGGER = 2;
    public static final int RISE = 8;
    public static final int RISEN = (COLUMNS - 1) * STAGGER + RISE;
    public static final int HOLD = 50;
    public static final int FALL = 12;
    public static final int IMPACT = RISEN + HOLD + FALL;
    public static final int RUBBLE = 30;
    public static final int RUBBLE_TICKS = 15;

    @Nullable
    private String spellId;

    public SunkenTemple(EntityType<? extends SunkenTemple> entityType, Level level) {
        super(entityType, level);
    }

    public static SunkenTemple raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float damage) {
        SunkenTemple temple = new SunkenTemple(ModEntities.SUNKEN_TEMPLE.get(), level);
        temple.setOwner(caster);
        temple.spellId = spell.getSpellId();
        temple.damage = damage;
        temple.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 45, 0);
        level.addFreshEntity(temple);
        return temple;
    }

    /** Where column {@code i} stands, relative to the middle. */
    public Vec3 column(int i) {
        float a = getYRot() * Mth.DEG_TO_RAD + i * Mth.TWO_PI / COLUMNS;
        return new Vec3(Mth.cos(a) * RADIUS, 0, Mth.sin(a) * RADIUS);
    }

    /** How far column {@code i} is out of the ground (0 to 1). */
    public float risen(int i, float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks - i * STAGGER) / RISE, 0, 1);
        float sink = Mth.clamp((tickCount + partialTicks - IMPACT - RUBBLE) / 12, 0, 1);
        return (1 - (1 - t) * (1 - t)) * (1 - sink);
    }

    /** How far the columns have toppled inward, in degrees (0 standing, 90 flat). */
    public float fallen(float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks - RISEN - HOLD) / FALL, 0, 1);
        return 90 * t * t;
    }

    /** 0 to 1 as the lintels come up on the columns, gone once the columns fall. */
    public float lintels(float partialTicks) {
        return Mth.clamp((tickCount + partialTicks - RISEN) / 6, 0, 1) * (fallen(partialTicks) > 0 ? 0 : 1);
    }

    private boolean inside(Entity e) {
        return e.distanceToSqr(getX(), e.getY(), getZ()) <= RADIUS * RADIUS && e.getY() > getY() - 2 && e.getY() < getY() + COLUMN_HEIGHT;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        Vec3 c = position();
        if (tickCount < RISEN && tickCount % STAGGER == 0 && tickCount / STAGGER < COLUMNS) {
            Vec3 at = c.add(column(tickCount / STAGGER));
            VerdigrisFx.debris(level, at.add(0, .4, 0), 18, .7);
            VerdigrisFx.rise(level, at, 1.4f);
        }
        if (tickCount == RISEN) {
            SpellUtil.cameraShake(level, c, 20, RADIUS + 14);
            VerdigrisFx.sound(level, c, SoundEvents.BELL_RESONATE, 2f, .4f);
        }
        if (tickCount > RISEN && tickCount < RISEN + HOLD) {
            shutIn(level);
            if ((tickCount - RISEN) % RUBBLE_TICKS == 0) {
                rubble(level);
            }
        }
        if (tickCount == RISEN + HOLD) {
            VerdigrisFx.sound(level, c, SoundEvents.COPPER_BREAK, 2.5f, .4f);
            VerdigrisFx.sound(level, c, SoundEvents.GRINDSTONE_USE, 2f, .4f);
        }
        if (tickCount == IMPACT) {
            collapse(level);
        }
        if (tickCount > IMPACT + RUBBLE + 12) {
            discard();
        }
    }

    @Nullable
    private AbstractSpell spell() {
        return spellId == null ? null : SpellRegistry.getSpell(spellId);
    }

    private void hurt(LivingEntity e, float amount) {
        AbstractSpell spell = spell();
        if (spell != null) {
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, this, getOwner()));
        }
    }

    /** The enemies inside cannot slip out between the columns: those at the edge are pushed back in. */
    private void shutIn(ServerLevel level) {
        Vec3 c = position();
        for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, 1, 0), RADIUS + 1.5, getOwner(), x -> x.getY() > c.y - 2 && x.getY() < c.y + COLUMN_HEIGHT)) {
            Vec3 d = e.position().subtract(c).multiply(1, 0, 1);
            double dist = d.length();
            if (dist > RADIUS - 1.2 && dist < RADIUS + 1.2 && dist > 1.0E-3) {
                Vec3 in = d.scale(-1 / dist);
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                e.setDeltaMovement(in.scale(.35 * Math.max(resist, .3)).add(0, e.getDeltaMovement().y, 0));
                e.hurtMarked = true;
            }
        }
    }

    /** Rubble falls from the lintels on those inside. */
    private void rubble(ServerLevel level) {
        float amount = damage * .2f;
        for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), RADIUS + 1, getOwner(), this::inside)) {
            hurt(e, amount);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
            VerdigrisFx.debris(level, e.position().add(0, e.getBbHeight() + .5, 0), 6, .4);
        }
        for (int i = 0; i < 6; i++) {
            Vec3 p = position().add((random.nextDouble() - .5) * RADIUS * 1.6, COLUMN_HEIGHT, (random.nextDouble() - .5) * RADIUS * 1.6);
            VerdigrisFx.debris(level, p, 4, .3);
        }
        VerdigrisFx.sound(level, position(), SoundEvents.GRAVEL_BREAK, 1.2f, .6f);
    }

    /** The columns fall in: everything inside is buried, those just outside hurt by the blast of dust and stone. */
    private void collapse(ServerLevel level) {
        Vec3 c = position();
        for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, 1, 0), RADIUS + 3, getOwner(), x -> x.getY() > c.y - 2 && x.getY() < c.y + COLUMN_HEIGHT)) {
            boolean in = inside(e);
            hurt(e, in ? damage * 2.5f : damage);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, in ? 80 : 40, in ? 3 : 1));
            e.setDeltaMovement(e.getDeltaMovement().multiply(.2, 0, .2).add(0, -.3, 0));
            e.hurtMarked = true;
        }
        for (int i = 0; i < COLUMNS; i++) {
            Vec3 at = c.add(column(i).scale(.5));
            VerdigrisFx.debris(level, at.add(0, .5, 0), 24, 1.6);
        }
        VerdigrisFx.sound(level, c, SoundEvents.GENERIC_EXPLODE.value(), 2f, .5f);
        VerdigrisFx.sound(level, c, SoundEvents.ANVIL_LAND, 2f, .4f);
        VerdigrisFx.sound(level, c, SoundEvents.COPPER_BREAK, 2.5f, .5f);
        SpellUtil.cameraShake(level, c, 30, RADIUS + 18);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
