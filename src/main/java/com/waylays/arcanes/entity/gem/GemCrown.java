package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gem.Gem;
import com.waylays.arcanes.spells.gem.GemFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * The Crown of the Five Gems (Gem Magic's ultimate). Five great gems rise round the caster at the points of a
 * pentagram drawn in light on the ground, and for eight seconds each does what it is: the ruby burns the nearest enemy
 * with its ray, the lapis seals one, the diamond hardens the caster's side, the amethyst rings out, the emerald heals.
 * Then the five rise spiralling and fuse into one prismatic gem high above, which strikes the spot the caster aimed at
 * with a ray of every colour, and everything there is blown away.
 */
public class GemCrown extends SpellEffectEntity {
    public static final float RADIUS = 4.5f;
    public static final float HEIGHT = 2.6f;
    public static final int RISE = 30;
    public static final int ACT = 160;
    public static final int FUSE = 36;
    public static final int STRIKE = RISE + ACT + FUSE;
    public static final int AFTER = 34;
    /** Each gem acts once a second, one after another. */
    public static final int ROUND = 20;
    public static final float REACH = 16;
    public static final float FUSE_HEIGHT = 10;
    public static final float BLAST = 7;

    private static final EntityDataAccessor<Vector3f> DATA_STRIKE = SynchedEntityData.defineId(GemCrown.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_RUBY_TARGET = SynchedEntityData.defineId(GemCrown.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LAPIS_TARGET = SynchedEntityData.defineId(GemCrown.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public GemCrown(EntityType<? extends GemCrown> entityType, Level level) {
        super(entityType, level);
    }

    public static GemCrown raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, Vec3 strike, float damage) {
        GemCrown crown = new GemCrown(ModEntities.GEM_CROWN.get(), level);
        crown.setOwner(caster);
        crown.spellId = spell.getSpellId();
        crown.damage = damage;
        crown.moveTo(at.x, at.y, at.z, caster.getYRot(), 0);
        crown.entityData.set(DATA_STRIKE, strike.toVector3f());
        level.addFreshEntity(crown);
        for (Gem gem : Gem.values()) {
            GemFx.chime(level, at, 1.4f, .6f + .2f * gem.ordinal());
        }
        return crown;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STRIKE, new Vector3f());
        builder.define(DATA_RUBY_TARGET, -1);
        builder.define(DATA_LAPIS_TARGET, -1);
    }

    /** Where the fused gem strikes. */
    public Vec3 getStrike() {
        return new Vec3(this.entityData.get(DATA_STRIKE));
    }

    public int getRubyTarget() {
        return this.entityData.get(DATA_RUBY_TARGET);
    }

    public int getLapisTarget() {
        return this.entityData.get(DATA_LAPIS_TARGET);
    }

    /** The tick gem {@code g} acts in round {@code r} (from 0). */
    public static int actsAt(Gem g, int round) {
        return RISE + round * ROUND + g.ordinal() * 4;
    }

    /**
     * Where gem {@code g} is, from the middle, at age {@code age}: rising to its point of the pentagram, floating there,
     * then spiralling up to fuse above.
     */
    public Vec3 gem(Gem g, float age) {
        float a = getYRot() * Mth.DEG_TO_RAD + Mth.HALF_PI + g.ordinal() * Mth.TWO_PI / 5;
        float rise = Mth.clamp((age - g.ordinal() * 3) / (RISE - 12), 0, 1);
        float y = Mth.lerp(rise * rise * (3 - 2 * rise), -.5f, HEIGHT) + .2f * Mth.sin(age * .08f + g.ordinal());
        float r = RADIUS;
        float fuse = Mth.clamp((age - RISE - ACT) / FUSE, 0, 1);
        if (fuse > 0) {
            float f = fuse * fuse;
            a += f * Mth.TWO_PI * 1.5f;
            r = Mth.lerp(f, RADIUS, 0);
            y = Mth.lerp(f, y, FUSE_HEIGHT);
        }
        return new Vec3(Mth.cos(a) * r, y, Mth.sin(a) * r);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > STRIKE + AFTER) {
            discard();
            return;
        }
        LivingEntity owner = getLivingOwner();
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (tickCount == RISE) {
            GemFx.sound(level, position(), SoundEvents.BEACON_ACTIVATE, 2f, 1.2f);
        }
        if (tickCount >= RISE && tickCount < RISE + ACT && owner != null && spell != null) {
            for (Gem g : Gem.values()) {
                int r = (tickCount - RISE) / ROUND;
                if (tickCount == actsAt(g, r)) {
                    act(level, owner, spell, g);
                }
            }
            if (tickCount % 20 == 0) {
                owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, true, true));
            }
        }
        if (tickCount == RISE + ACT) {
            GemFx.sound(level, position(), SoundEvents.BEACON_POWER_SELECT, 2f, .6f);
        }
        if (tickCount == STRIKE && spell != null) {
            strike(level, owner, spell);
        }
    }

    /** What gem {@code g} does in its turn. */
    private void act(ServerLevel level, LivingEntity owner, AbstractSpell spell, Gem g) {
        Vec3 from = position().add(gem(g, tickCount));
        List<LivingEntity> foes = SpellUtil.enemiesAround(level, position(), REACH, owner, e -> e.hasLineOfSight(owner) || owner.hasLineOfSight(e));
        foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
        switch (g) {
            case RUBY -> {
                LivingEntity foe = foes.isEmpty() ? null : foes.get(0);
                this.entityData.set(DATA_RUBY_TARGET, foe == null ? -1 : foe.getId());
                if (foe != null) {
                    foe.invulnerableTime = 0;
                    DamageSources.applyDamage(foe, damage * .5f, SpellUtil.damageSource(spell, this, owner));
                    foe.igniteForSeconds(3);
                    GemFx.glitter(level, foe.getBoundingBox().getCenter(), Gem.RUBY, 8, .3);
                    GemFx.sound(level, from, SoundEvents.BLAZE_SHOOT, .8f, 1.6f);
                }
            }
            case LAPIS -> {
                LivingEntity foe = foes.stream().filter(e -> !e.hasEffect(ModEffects.SILENCE)).findFirst().orElse(foes.isEmpty() ? null : foes.get(0));
                this.entityData.set(DATA_LAPIS_TARGET, foe == null ? -1 : foe.getId());
                if (foe != null) {
                    foe.addEffect(new MobEffectInstance(ModEffects.SILENCE, 70, 0));
                    foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3));
                    GemFx.glitter(level, foe.getBoundingBox().getCenter(), Gem.LAPIS, 8, .4);
                    GemFx.sound(level, foe.position(), SoundEvents.CHAIN_PLACE, 1f, .8f);
                }
            }
            case DIAMOND -> {
                for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(10),
                        e -> e.isAlive() && SpellUtil.isAlly(owner, e))) {
                    ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, true, true));
                    GemFx.glitter(level, ally.getBoundingBox().getCenter(), Gem.DIAMOND, 4, .4);
                }
            }
            case AMETHYST -> {
                for (LivingEntity foe : SpellUtil.enemiesAround(level, position().add(0, 1, 0), RADIUS + 3, owner, e -> true)) {
                    foe.invulnerableTime = 0;
                    DamageSources.applyDamage(foe, damage * .3f, SpellUtil.damageSource(spell, this, owner));
                    foe.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                }
                GemFx.chime(level, from, 1.2f, 1.2f);
            }
            case EMERALD -> {
                for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(10),
                        e -> e.isAlive() && SpellUtil.isAlly(owner, e))) {
                    ally.heal(2);
                    GemFx.glitter(level, ally.getBoundingBox().getCenter(), Gem.EMERALD, 4, .4);
                }
            }
        }
    }

    /** The fused gem strikes the spot aimed at. */
    private void strike(ServerLevel level, @Nullable LivingEntity owner, AbstractSpell spell) {
        Vec3 at = getStrike();
        for (LivingEntity e : SpellUtil.enemiesAround(level, at.add(0, 1, 0), BLAST, owner, x -> true)) {
            float falloff = (float) Mth.clamp(1 - .5 * e.position().distanceTo(at) / BLAST, .5, 1);
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, damage * 3 * falloff, SpellUtil.damageSource(spell, this, owner));
            Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            e.setDeltaMovement(away.scale(1.4 * resist).add(0, .8 * resist, 0));
            e.hurtMarked = true;
        }
        for (Gem g : Gem.values()) {
            GemFx.shatter(level, at.add(0, 1, 0), g, 2.6f, 10);
        }
        GemFx.shatter(level, position().add(0, FUSE_HEIGHT, 0), Gem.DIAMOND, 2f, 16);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3f, .7f);
        GemFx.sound(level, at, SoundEvents.BEACON_DEACTIVATE, 2f, 1.4f);
        SpellUtil.cameraShake(level, at, 30, BLAST + 20);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }
}
