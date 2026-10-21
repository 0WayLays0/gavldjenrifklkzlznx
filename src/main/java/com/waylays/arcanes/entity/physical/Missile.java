package com.waylays.arcanes.entity.physical;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.physical.MissileSkill;
import com.waylays.arcanes.spells.physical.PhysicalSkill;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Whatever a fighting move shoots or throws: an arrow, a crossbow bolt, a throwing knife, an axe, a javelin, a
 * harpoon. It flies like an arrow (it falls, slows down, sticks in walls) but nobody can pick it up, and what it does
 * to whoever it hits belongs to the move that threw it (a {@link MissileSkill}): the damage it carries, how many it
 * goes through, effects, a pull. It never hurts its thrower's allies, and one stuck in a wall goes away after a few
 * seconds.
 */
public class Missile extends AbstractArrow {
    public enum Kind { ARROW, BOLT, KNIFE, AXE, JAVELIN, HARPOON }

    /** How long a missile stays stuck in a wall. */
    public static final int STUCK_TICKS = 60;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(Missile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(Missile.class, EntityDataSerializers.ITEM_STACK);

    @Nullable
    private ResourceLocation skillId;
    private int skillLevel = 1;
    private float damage;
    private int pierce;
    private int maxFlight;
    private int flightTicks;
    private int stuckTicks;
    private final IntOpenHashSet struck = new IntOpenHashSet();

    public Missile(EntityType<? extends Missile> entityType, Level level) {
        super(entityType, level);
        this.pickup = Pickup.DISALLOWED;
    }

    /**
     * Shoots a missile of {@code skill} from {@code from} along {@code direction} at {@code speed} blocks a tick,
     * carrying {@code damage}.
     */
    public static Missile shoot(LivingEntity owner, Kind kind, PhysicalSkill skill, int skillLevel, float damage, Vec3 from, Vec3 direction, float speed,
                                float inaccuracy) {
        Missile missile = new Missile(ModEntities.MISSILE.get(), owner.level());
        missile.setOwner(owner);
        missile.setPos(from);
        missile.entityData.set(DATA_KIND, kind.ordinal());
        missile.skillId = skill.getSpellResource();
        missile.skillLevel = skillLevel;
        missile.damage = damage;
        missile.shoot(direction.x, direction.y, direction.z, speed, inaccuracy);
        owner.level().addFreshEntity(missile);
        return missile;
    }

    /** Where a fighter's missiles leave from: their chest, a little ahead. */
    public static Vec3 launchPoint(LivingEntity fighter) {
        return fighter.getEyePosition().subtract(0, .15, 0).add(fighter.getLookAngle().scale(.3));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KIND, 0);
        builder.define(DATA_ITEM, ItemStack.EMPTY);
    }

    public Kind getKind() {
        Kind[] kinds = Kind.values();
        return kinds[Math.floorMod(this.entityData.get(DATA_KIND), kinds.length)];
    }

    /** The item a thrown weapon looks like (an axe throw shows the thrower's own axe). */
    public ItemStack getItem() {
        return this.entityData.get(DATA_ITEM);
    }

    public Missile withItem(ItemStack stack) {
        this.entityData.set(DATA_ITEM, stack.copyWithCount(1));
        return this;
    }

    /** Goes through {@code count} more enemies before it stops. */
    public Missile withPierce(int count) {
        this.pierce = count;
        // Lets the arrow's own hit search go on past the first one it meets in a tick: a fast missile can cross two
        setPierceLevel((byte) Math.min(Byte.MAX_VALUE, count));
        return this;
    }

    /** Drops out of the air after {@code ticks} of flight. */
    public Missile withMaxFlight(int ticks) {
        this.maxFlight = ticks;
        return this;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public float getDamage() {
        return damage;
    }

    public int getFlightTicks() {
        return flightTicks;
    }

    public boolean isStuck() {
        return inGround;
    }

    @Nullable
    private MissileSkill skill() {
        AbstractSpell spell = skillId == null ? null : SpellRegistry.getSpell(skillId);
        return spell instanceof MissileSkill missileSkill ? missileSkill : null;
    }

    /**
     * Hurts {@code target} with {@code amount} of the move's physical damage (armor reduces it, a raised shield can
     * stop it) and shoves it along the flight by {@code knockback}.
     */
    public boolean strike(LivingEntity target, float amount, double knockback) {
        if (!(skill() instanceof PhysicalSkill skill)) {
            return false;
        }
        Entity owner = getOwner();
        // Several missiles of a volley can each land
        target.invulnerableTime = 0;
        boolean hurt = DamageSources.applyDamage(target, amount, skill.getDamageSource(this, owner != null ? owner : this));
        if (hurt && knockback > 0) {
            Vec3 push = getDeltaMovement().multiply(1, 0, 1);
            if (push.lengthSqr() > 1.0E-6) {
                push = push.normalize().scale(knockback * .6);
                target.push(push.x, .1, push.z);
                target.hurtMarked = true;
            }
        }
        return hurt;
    }

    @Override
    public void tick() {
        if (!level().isClientSide && !inGround) {
            MissileSkill skill = skill();
            if (skill != null) {
                skill.tickMissile(this);
            }
            flightTicks++;
            if (maxFlight > 0 && flightTicks > maxFlight) {
                discard();
                return;
            }
        }
        super.tick();
    }

    @Override
    protected void tickDespawn() {
        if (++stuckTicks >= STUCK_TICKS) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !struck.contains(entity.getId()) && !SpellUtil.isAlly(getOwner(), entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hit = result.getEntity();
        // On both sides: a piercing arrow looks again for what it hits until it finds nothing new (an endless loop on
        // the client if what it went through is not set aside there too)
        struck.add(hit.getId());
        if (level().isClientSide) {
            return;
        }
        MissileSkill skill = skill();
        if (hit instanceof LivingEntity target && skill != null) {
            skill.onMissileHit(this, target);
        }
        if (pierce-- <= 0) {
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        MissileSkill skill = skill();
        if (!level().isClientSide && skill != null) {
            skill.onMissileHitBlock(this, result);
        }
        if (!isRemoved()) {
            super.onHitBlock(result);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        // Never picked up (pickup is disallowed), but the arrow's save needs an item: an empty one cannot be written
        return new ItemStack(Items.ARROW);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", this.entityData.get(DATA_KIND));
        if (!getItem().isEmpty()) {
            tag.put("Item", getItem().save(registryAccess()));
        }
        if (skillId != null) {
            tag.putString("Skill", skillId.toString());
        }
        tag.putInt("SkillLevel", skillLevel);
        tag.putFloat("Damage", damage);
        tag.putInt("Pierce", pierce);
        tag.putInt("MaxFlight", maxFlight);
        tag.putInt("FlightTicks", flightTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_KIND, tag.getInt("Kind"));
        if (tag.contains("Item")) {
            this.entityData.set(DATA_ITEM, ItemStack.parseOptional(registryAccess(), tag.getCompound("Item")));
        }
        skillId = tag.contains("Skill") ? ResourceLocation.tryParse(tag.getString("Skill")) : null;
        skillLevel = Math.max(1, tag.getInt("SkillLevel"));
        damage = tag.getFloat("Damage");
        pierce = tag.getInt("Pierce");
        maxFlight = tag.getInt("MaxFlight");
        flightTicks = tag.getInt("FlightTicks");
    }
}
