package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.entity.wall.Masonry;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Sudden Grove: a ring of real trees (oak trunks, crowns of azalea in flower) springs out of the ground round the caster
 * in a moment, one after another, each trunk shooting up a block a tick: what stood where one burst out is thrown up and
 * hurt. Inside the grove the caster and their allies are mended, and enemies are tangled in roots. When its time is up
 * the trees are gone without a trace (only into air and what can be replaced, never into a build: see {@link Masonry}).
 */
public class SuddenGrove extends SpellEffectEntity {
    public static final int GAP = 3;
    public static final float RADIUS = 8;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(SuddenGrove.class, EntityDataSerializers.INT);

    /** One tree: where its foot is, how tall its trunk, the tick it starts to grow. */
    private record Tree(BlockPos foot, int height, int start) {}

    private final Masonry masonry = new Masonry();
    private final List<Tree> trees = new ArrayList<>();
    @Nullable
    private String spellId;
    private float mend;
    private boolean gone;

    public SuddenGrove(EntityType<? extends SuddenGrove> entityType, Level level) {
        super(entityType, level);
    }

    public static SuddenGrove plant(Level level, LivingEntity caster, AbstractSpell spell, int count, int life, float damage, float mend) {
        SuddenGrove grove = new SuddenGrove(ModEntities.SUDDEN_GROVE.get(), level);
        grove.setOwner(caster);
        grove.spellId = spell.getSpellId();
        grove.damage = damage;
        grove.mend = mend;
        Vec3 at = caster.position();
        grove.moveTo(at.x, at.y, at.z, caster.getYRot(), 0);
        grove.entityData.set(DATA_LIFE, life);
        float yaw = caster.getYRot() * Mth.DEG_TO_RAD;
        for (int k = 0; k < count; k++) {
            float a = yaw + k * Mth.TWO_PI / count + (level.random.nextFloat() - .5f) * .3f;
            float r = 4 + level.random.nextFloat() * 2.5f;
            Vec3 spot = SpellUtil.groundAt(level, at.add(-Mth.sin(a) * r, 0, Mth.cos(a) * r), 4, 6);
            grove.trees.add(new Tree(BlockPos.containing(spot), 4 + level.random.nextInt(3), 4 + k * GAP));
        }
        level.addFreshEntity(grove);
        WoodFx.sprout(level, at, 1.5f);
        WoodFx.life(level, at.add(0, .5, 0), 30, 3);
        return grove;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 400);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    private static BlockState leaves(int seed) {
        BlockState state = (WoodFx.hash(seed) < .35f ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES).defaultBlockState();
        return state.setValue(LeavesBlock.PERSISTENT, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Entity caster = getOwner();
        if (!gone && tickCount < getLife()) {
            for (Tree t : trees) {
                grow(level, t, caster);
            }
            if (tickCount % 20 == 0) {
                tend(level);
            }
        }
        if (tickCount >= getLife() && !gone) {
            gone = true;
            for (Masonry.Laid l : masonry.laid()) {
                if (random.nextInt(4) == 0) {
                    WoodFx.leaves(level, Vec3.atCenterOf(l.pos()), 3, .4);
                }
            }
            masonry.restore(level);
            WoodFx.creak(level, position(), 1.6f);
            WoodFx.sound(level, position(), SoundEvents.AZALEA_LEAVES_BREAK, 2, .6f);
            WoodFx.life(level, position().add(0, 1, 0), 40, 4);
        }
        if (tickCount > getLife() + 10) {
            discard();
        }
    }

    /** A tree's trunk shoots up a block a tick; its crown comes out all at once on top. */
    private void grow(ServerLevel level, Tree t, @Nullable Entity caster) {
        int step = tickCount - t.start();
        if (step < 0 || step > t.height()) {
            return;
        }
        if (step == 0) {
            Vec3 foot = Vec3.atBottomCenterOf(t.foot());
            WoodFx.sprout(level, foot, 1.2f);
            WoodFx.chips(level, foot.add(0, .3, 0), 12, .5);
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            if (spell != null) {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(t.foot()).inflate(.8, 1, .8))) {
                    if (!e.isAlive() || e == caster || SpellUtil.isAlly(caster, e)) {
                        continue;
                    }
                    e.invulnerableTime = 0;
                    DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                    double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                    e.setDeltaMovement(e.getDeltaMovement().add(0, .8 * Math.max(.4, resist), 0));
                    e.hurtMarked = true;
                }
            }
        }
        if (step < t.height()) {
            masonry.lay(level, t.foot().above(step), Blocks.OAK_LOG.defaultBlockState(), caster, Vec3.atCenterOf(t.foot()));
            if (step % 2 == 0) {
                WoodFx.sound(level, Vec3.atCenterOf(t.foot().above(step)), SoundEvents.WOOD_PLACE, 1, .7f + step * .05f);
            }
            return;
        }
        // The crown: a round heap of leaves round the top of the trunk
        BlockPos top = t.foot().above(t.height() - 1);
        int seed = (int) top.asLong();
        for (int dy = -1; dy <= 2; dy++) {
            int r = dy == 2 ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean corner = Math.abs(dx) == r && Math.abs(dz) == r;
                    if (dx == 0 && dz == 0 && dy <= 0 || corner && (dy != 0 || WoodFx.hash(seed + dx * 7 + dz * 13) < .5f)) {
                        continue;
                    }
                    masonry.lay(level, top.offset(dx, dy, dz), leaves(seed + dx * 31 + dy * 17 + dz * 5), caster, Vec3.atCenterOf(top));
                }
            }
        }
        WoodFx.sound(level, Vec3.atCenterOf(top), SoundEvents.AZALEA_LEAVES_PLACE, 1.5f, .8f);
        WoodFx.leaves(level, Vec3.atCenterOf(top), 16, 1.5);
    }

    /** Allies inside are mended; enemies are tangled in roots. */
    private void tend(ServerLevel level) {
        LivingEntity caster = getLivingOwner();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RADIUS, 4, RADIUS))) {
            if (!e.isAlive() || e.distanceToSqr(this) > RADIUS * RADIUS) {
                continue;
            }
            if (e == caster || SpellUtil.isAlly(caster, e)) {
                if (e.getHealth() < e.getMaxHealth()) {
                    e.heal(mend);
                    WoodFx.life(level, e.position().add(0, 1, 0), 5, .4);
                }
            } else {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0));
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && level() instanceof ServerLevel level) {
            masonry.restore(level);
        }
        super.remove(reason);
    }

    @Override
    public boolean shouldBeSaved() {
        return !isRemoved();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Life", getLife());
        tag.putFloat("Mend", mend);
        if (spellId != null) {
            tag.putString("Spell", spellId);
        }
        masonry.save(tag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_LIFE, tag.getInt("Life"));
        mend = tag.getFloat("Mend");
        spellId = tag.contains("Spell") ? tag.getString("Spell") : null;
        // Trees still to grow are not kept: the grove stands as it was
        trees.clear();
        if (level() instanceof ServerLevel level) {
            masonry.load(tag, level);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
