package com.waylays.arcanes.entity.wall;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wall.WallFx;
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
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Tyrant's Fortress (Wall Magic's ultimate): the ground shakes and a fortress rises around the caster, its round
 * wall built brick by brick running all the way round (real blocks: nobody gets in), eight towers bursting up along
 * it with braziers of embers on top. For ten seconds the towers shell every enemy around, inside the walls and out,
 * one tower after another, while the caster stands behind them unshaken. Then the whole fortress blows apart outward,
 * its towers bursting and its wall crumbling, the blast throwing back everything around.
 */
public class Citadel extends SpellEffectEntity {
    public static final int TOWERS = 8;
    public static final int RADIUS = 9;
    public static final int WALL_HEIGHT = 4;
    public static final float TOWER_HEIGHT = 8;
    public static final int TOWER_STAGGER = 4;
    public static final int TOWER_RISE = 12;
    public static final int RISEN = (TOWERS - 1) * TOWER_STAGGER + TOWER_RISE;
    public static final int HOLD = 200;
    public static final int COLLAPSE = RISEN + HOLD;
    public static final int AFTER = 30;
    public static final int VOLLEY_TICKS = 12;
    public static final float SHELL_RANGE = 26;
    public static final float BLAST = RADIUS + 8;

    private static final EntityDataAccessor<Integer> DATA_COLLAPSED = SynchedEntityData.defineId(Citadel.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    @Nullable
    private Rampart wall;
    private int volley;

    public Citadel(EntityType<? extends Citadel> entityType, Level level) {
        super(entityType, level);
    }

    public static Citadel raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float damage) {
        Citadel citadel = new Citadel(ModEntities.CITADEL.get(), level);
        citadel.setOwner(caster);
        citadel.spellId = spell.getSpellId();
        citadel.damage = damage;
        citadel.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 45, 0);
        level.addFreshEntity(citadel);
        citadel.wall = Rampart.raise(level, caster, Rampart.Shape.RING, at, 0, RADIUS, WALL_HEIGHT, COLLAPSE);
        return citadel;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COLLAPSED, -1);
    }

    /** Where tower {@code i} stands, relative to the middle. */
    public Vec3 tower(int i) {
        float a = getYRot() * Mth.DEG_TO_RAD + i * Mth.TWO_PI / TOWERS;
        return new Vec3(Mth.cos(a) * RADIUS, 0, Mth.sin(a) * RADIUS);
    }

    /** How far tower {@code i} is out of the ground (0 to 1, with an overshoot as it bursts out). */
    public float risen(int i, float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks - i * TOWER_STAGGER) / TOWER_RISE, 0, 1);
        return t * (1 + .1f * Mth.sin(t * Mth.PI));
    }

    /** The tick it blew apart, or -1. */
    public int getCollapsed() {
        return this.entityData.get(DATA_COLLAPSED);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (getCollapsed() >= 0) {
            if (tickCount > getCollapsed() + AFTER) {
                discard();
            }
            return;
        }
        if (tickCount < RISEN && tickCount % TOWER_STAGGER == 0) {
            Vec3 t = position().add(tower(tickCount / TOWER_STAGGER));
            WallFx.rise(level, t, 2f);
            WallFx.dust(level, t.add(0, .5, 0), 30, 1.2);
            WallFx.embers(level, t.add(0, .5, 0), 10, 1);
            SpellUtil.cameraShake(level, t, 6, 20);
        }
        LivingEntity caster = getLivingOwner();
        if (tickCount > RISEN && tickCount < COLLAPSE) {
            if (caster != null && tickCount % 20 == 0) {
                caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, true, true));
            }
            if ((tickCount - RISEN) % VOLLEY_TICKS == 0) {
                shell(level, caster);
            }
        }
        if (tickCount == COLLAPSE) {
            collapse(level);
        }
    }

    /** The next tower lobs a mortar at an enemy in range, taking them in turn from the nearest out. */
    private void shell(ServerLevel level, @Nullable LivingEntity caster) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null || caster == null) {
            return;
        }
        List<LivingEntity> foes = SpellUtil.enemiesAround(level, position().add(0, 1, 0), SHELL_RANGE, caster, e -> true);
        if (foes.isEmpty()) {
            return;
        }
        // Each volley at the next enemy out from the middle, the nearest first: inside the walls before beyond them
        int n = volley++;
        int t = n % TOWERS;
        Vec3 middle = position();
        foes.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(middle)));
        LivingEntity target = foes.get(n % foes.size());
        Vec3 from = position().add(tower(t)).add(0, TOWER_HEIGHT + 1, 0);
        Vec3 to = target.position();
        int ticks = 18;
        double g = .05;
        Vec3 d = to.subtract(from);
        Vec3 velocity = new Vec3(d.x / ticks, d.y / ticks + .5 * g * (ticks - 1), d.z / ticks);
        BrickBolt.shoot(level, caster, spell, BrickBolt.Kind.MORTAR, from, velocity, to, damage * .6f);
        WallFx.embers(level, from, 12, .4);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.6f, .5f);
    }

    /** The fortress blows apart outward: towers burst, the wall crumbles, the blast throws back everything around. */
    private void collapse(ServerLevel level) {
        this.entityData.set(DATA_COLLAPSED, tickCount);
        if (wall != null) {
            wall.crumbleNow();
        }
        Vec3 c = position();
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, 1, 0), BLAST, getOwner(), x -> x.getY() > c.y - 3 && x.getY() < c.y + TOWER_HEIGHT + 2)) {
            if (spell != null) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage * 2, SpellUtil.damageSource(spell, this, getOwner()));
            }
            Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            e.setDeltaMovement(away.scale(1.6 * resist).add(0, .7 * resist, 0));
            e.hurtMarked = true;
        }
        for (int i = 0; i < TOWERS; i++) {
            WallFx.impact(level, c.add(tower(i)), 3, 14);
        }
        WallFx.impact(level, c, RADIUS, 0);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3f, .5f);
        WallFx.slam(level, c, 3f);
        SpellUtil.cameraShake(level, c, 40, BLAST + 24);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && wall != null && !wall.isRemoved() && getCollapsed() < 0) {
            wall.crumbleNow();
        }
        super.remove(reason);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }
}
