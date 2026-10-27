package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * The World Tree, the ultimate of Wood Magic: the ground heaves and a colossal tree tears its way up out of it, its
 * trunk a braid of living wood, its roots spreading far over the ground, its crown in flower far above: what stood where
 * it broke out is hurled away, and the quake runs out a long way round. While it stands its roots lash out of the ground
 * at the enemies round it, again and again, seizing them; its friends under its crown are healed and its enemies weighed
 * down. When its time is up it sheds its leaves and sinks back into the earth.
 */
public class WorldTree extends SpellEffectEntity {
    public static final int RISE = 50;
    /** The tick its trunk breaks out of the ground. */
    public static final int BREAK_OUT = 14;
    public static final int WITHER = 40;
    public static final float HEIGHT = 24;
    public static final float CROWN = 11;
    public static final float REACH = 18;
    public static final float QUAKE = 12;
    public static final int LASH_EVERY = 30;
    public static final int LASH_TICKS = 16;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(WorldTree.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> DATA_LASH = SynchedEntityData.defineId(WorldTree.class, EntityDataSerializers.COMPOUND_TAG);

    @Nullable
    private String spellId;
    private int lashes = 3;
    private float mend;

    public WorldTree(EntityType<? extends WorldTree> entityType, Level level) {
        super(entityType, level);
    }

    public static WorldTree raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, int life, int lashes, float damage, float mend) {
        WorldTree tree = new WorldTree(ModEntities.WORLD_TREE.get(), level);
        tree.setOwner(caster);
        tree.spellId = spell.getSpellId();
        tree.damage = damage;
        tree.lashes = lashes;
        tree.mend = mend;
        tree.moveTo(at.x, at.y, at.z, caster.getYRot(), 0);
        tree.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(tree);
        WoodFx.creak(level, at, 2.5f);
        WoodFx.sound(level, at, SoundEvents.WARDEN_EMERGE, 1.5f, .6f);
        return tree;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 600);
        builder.define(DATA_LASH, new CompoundTag());
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** The tick of the last lash of its roots, and whom they struck. */
    public int getLashTick() {
        CompoundTag tag = this.entityData.get(DATA_LASH);
        return tag.contains("T") ? tag.getInt("T") : -100;
    }

    public int[] getLashed() {
        return this.entityData.get(DATA_LASH).getIntArray("Ids");
    }

    /** How far it has risen (0 to 1), and how far it has sunk back at the end. */
    public float risen(float age) {
        float t = Mth.clamp((age - BREAK_OUT + 6) / (RISE - BREAK_OUT + 6), 0, 1);
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    public float withered(float age) {
        return Mth.clamp((age - getLife()) / WITHER, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > getLife() + WITHER) {
            discard();
            return;
        }
        LivingEntity caster = getLivingOwner();
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (tickCount < BREAK_OUT) {
            // The ground heaves
            if (tickCount % 3 == 0) {
                WoodFx.chips(level, position().add(0, .2, 0), 20, 2.5);
                WoodFx.sound(level, position(), SoundEvents.ROOTED_DIRT_BREAK, 2, .4f);
                SpellUtil.cameraShake(level, position(), 4, 30);
            }
            return;
        }
        if (tickCount == BREAK_OUT) {
            breakOut(level, caster, spell);
        }
        if (tickCount < RISE) {
            if (tickCount % 4 == 0) {
                WoodFx.creak(level, position().add(0, HEIGHT * risen(tickCount), 0), 2);
                WoodFx.chips(level, position().add(0, 1, 0), 16, 2);
            }
            return;
        }
        if (tickCount == getLife()) {
            WoodFx.leaves(level, position().add(0, HEIGHT * .8, 0), 120, CROWN * .6);
            WoodFx.creak(level, position().add(0, 6, 0), 3);
        }
        if (tickCount >= getLife()) {
            if (tickCount % 4 == 0) {
                WoodFx.leaves(level, position().add(0, HEIGHT * .7, 0), 20, CROWN * .7);
            }
            return;
        }
        if (tickCount % 20 == 0) {
            tend(level, caster);
        }
        if (tickCount % 6 == 0) {
            WoodFx.life(level, position().add(0, 3, 0), 4, 3);
        }
        if ((tickCount - RISE) % LASH_EVERY == 0 && spell != null) {
            lash(level, caster, spell);
        }
    }

    /** Its trunk breaks out of the ground: what stood there is hurled away, and the quake runs out round it. */
    private void breakOut(ServerLevel level, @Nullable LivingEntity caster, @Nullable AbstractSpell spell) {
        if (spell != null) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, position(), QUAKE, caster, x -> Math.abs(x.getY() - getY()) < 5)) {
                double d = Math.hypot(e.getX() - getX(), e.getZ() - getZ());
                boolean under = d < 4;
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, under ? damage * 1.6f : damage * (1 - (float) (d / QUAKE) * .5f), SpellUtil.damageSource(spell, this, caster));
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                Vec3 out = e.position().subtract(position()).multiply(1, 0, 1);
                out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(out.scale((under ? 1.6 : .9) * resist)).add(0, (under ? 1.1 : .5) * Math.max(.4, resist), 0));
                e.hurtMarked = true;
            }
        }
        WoodFx.crash(level, position(), 3);
        WoodFx.sound(level, position(), SoundEvents.GENERIC_EXPLODE.value(), 1.5f, .45f);
        WoodFx.sound(level, position(), SoundEvents.WARDEN_SONIC_BOOM, .6f, .5f);
        WoodFx.chips(level, position().add(0, .5, 0), 80, 4);
        WoodFx.leaves(level, position().add(0, 2, 0), 40, 4);
        SpellUtil.cameraShake(level, position(), 20, 40);
    }

    /** Friends under its crown are healed; enemies there are weighed down. */
    private void tend(ServerLevel level, @Nullable LivingEntity caster) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(CROWN + 3, 12, CROWN + 3))) {
            if (!e.isAlive() || Math.hypot(e.getX() - getX(), e.getZ() - getZ()) > CROWN + 3) {
                continue;
            }
            if (e == caster || SpellUtil.isAlly(caster, e)) {
                if (e.getHealth() < e.getMaxHealth()) {
                    e.heal(mend);
                }
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, false, true));
                WoodFx.life(level, e.position().add(0, 1, 0), 6, .4);
            } else {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
        }
    }

    /** Its roots lash out of the ground at the nearest enemies and seize them. */
    private void lash(ServerLevel level, @Nullable LivingEntity caster, AbstractSpell spell) {
        List<LivingEntity> targets = SpellUtil.enemiesAround(level, position(), REACH, caster, x -> Math.abs(x.getY() - getY()) < 8);
        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
        int n = Math.min(lashes, targets.size());
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) {
            LivingEntity e = targets.get(i);
            ids[i] = e.getId();
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, damage * .6f, SpellUtil.damageSource(spell, this, caster));
            if (caster != null) {
                RootSnare.seize(level, caster, spell, e, 30, damage * .2f);
            }
            WoodFx.chips(level, e.position().add(0, .3, 0), 12, .5);
        }
        if (n > 0) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("T", tickCount);
            tag.putIntArray("Ids", ids);
            this.entityData.set(DATA_LASH, tag);
            WoodFx.sprout(level, position(), 2);
            WoodFx.sound(level, position(), SoundEvents.WARDEN_ATTACK_IMPACT, 1.2f, .5f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }
}
