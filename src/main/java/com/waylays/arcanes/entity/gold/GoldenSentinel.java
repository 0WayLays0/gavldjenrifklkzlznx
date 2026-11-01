package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.entity.OwnedSummon;
import com.waylays.arcanes.entity.OwnerTargetGoal;
import com.waylays.arcanes.entity.SummonFollowOwnerGoal;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gold.GoldFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A golden knight summoned by Golden Sentinels, crowned with the sun of Eldorado. It rises out of molten gold (still
 * and untouchable for the moment it takes), then guards its summoner: attacks what the summoner attacks, what attacks
 * the summoner and nearby monsters. Shatters into coins when its time runs out or
 * when it dies.
 */
public class GoldenSentinel extends PathfinderMob implements OwnedSummon {
    @Nullable
    private UUID ownerUUID;
    private int lifeTicks = 600;

    /** Ticks it takes to rise out of the gold. */
    public static final int EMERGE_TICKS = 24;

    public GoldenSentinel(EntityType<? extends GoldenSentinel> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.MOVEMENT_SPEED, .3)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, .4);
    }

    /** Called once by the spell: owner, strength and golden gear. */
    public void setup(LivingEntity owner, int lifeTicks, double health, double damage) {
        this.ownerUUID = owner.getUUID();
        this.lifeTicks = lifeTicks;
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(health);
        }
        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(damage);
        }
        setHealth(getMaxHealth());
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0);
        }
    }

    @Override
    @Nullable
    public UUID getSummonerUUID() {
        return ownerUUID;
    }

    @Override
    @Nullable
    public LivingEntity getSummoner() {
        if (ownerUUID != null && level() instanceof ServerLevel serverLevel && serverLevel.getEntity(ownerUUID) instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(2, new SummonFollowOwnerGoal(this, 1.15, 6, 3, -1));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, .8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerTargetGoal(this, true));
        this.targetSelector.addGoal(2, new OwnerTargetGoal(this, false));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
                e -> e instanceof Enemy && !SpellUtil.isAlly(getSummoner(), e)));
    }

    /** Rising out of the gold, it neither thinks nor moves. */
    @Override
    protected boolean isImmobile() {
        return tickCount < EMERGE_TICKS || super.isImmobile();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return (tickCount >= EMERGE_TICKS || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) && super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount < EMERGE_TICKS) {
                level().addParticle(random.nextInt(3) == 0 ? GoldFx.sparks() : GoldFx.MOLTEN_DUST, getRandomX(.8), getY() + .05, getRandomZ(.8),
                        0, .08 + random.nextDouble() * .1, 0);
            }
            if (random.nextInt(8) == 0) {
                level().addParticle(GoldFx.PALE_DUST, getRandomX(.6), getY() + random.nextDouble() * getBbHeight(), getRandomZ(.6), 0, .02, 0);
            }
            return;
        }
        if (--lifeTicks <= 0) {
            shatter();
        }
        LivingEntity target = getTarget();
        if (target != null && SpellUtil.isAlly(getSummoner(), target)) {
            setTarget(null);
        }
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.GOLDEN_SENTINELS.get();
    }

    @Override
    public void dismissSummon() {
        shatter();
    }

    /** Bursts into gold and disappears (no drops). */
    public void shatter() {
        if (!level().isClientSide) {
            GoldFx.shatterBurst(level(), getX(), getY() + 1, getZ(), 1.2f);
            GoldFx.coins(level(), position().add(0, 1.2, 0), 10, .3f);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.5f, .8f);
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            shatter();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            SpellUtil.particles(level(), GoldFx.sparks(), target.getX(), target.getY() + target.getBbHeight() * .6, target.getZ(), 5, .2, .2, .2, .25);
        }
        return hit;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (ownerUUID != null && (ownerUUID.equals(other.getUUID())
                || other instanceof OwnedSummon summon && ownerUUID.equals(summon.getSummonerUUID()))) {
            return true;
        }
        return super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isAlliedTo(target) && super.canAttack(target);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ANVIL_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.CHAIN_STEP, .4f, .8f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = tag.getInt("LifeTicks");
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }
}
