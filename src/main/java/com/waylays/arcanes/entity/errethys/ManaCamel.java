package com.waylays.arcanes.entity.errethys;

import com.waylays.arcanes.entity.OwnedSummon;
import com.waylays.arcanes.spells.errethys.ErrethysFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * The mana dromedary: a camel of sky-blue mana, saddled and tame from the start. Its summoner rides it (right click)
 * with all of a camel's moves, dash included; once the summoner sits on it a second rider may climb on behind. No
 * inventory, no breeding, no drops. It fades when its time is up, setting its riders down.
 */
public class ManaCamel extends Camel implements OwnedSummon {
    private int lifeTicks = 1800;

    public ManaCamel(EntityType<? extends ManaCamel> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
    }

    /** Called once by the spell. */
    public void setup(LivingEntity owner, int lifeTicks, double health) {
        setOwnerUUID(owner.getUUID());
        setTamed(true);
        this.lifeTicks = lifeTicks;
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(health);
        }
        setHealth(getMaxHealth());
    }

    public int getLifeTicks() {
        return lifeTicks;
    }

    @Override
    @Nullable
    public LivingEntity getOwner() {
        if (getOwnerUUID() != null && level() instanceof ServerLevel serverLevel && serverLevel.getEntity(getOwnerUUID()) instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    public boolean isSaddled() {
        return true;
    }

    @Override
    public boolean isSaddleable() {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
    }

    /** The summoner climbs on; anyone else only behind the summoner. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isPassenger() || isBaby()) {
            return InteractionResult.PASS;
        }
        boolean owner = player.getUUID().equals(getOwnerUUID());
        Entity front = getFirstPassenger();
        boolean ownerAboard = front != null && front.getUUID().equals(getOwnerUUID());
        if (owner && !isVehicle() || ownerAboard && canAddPassenger(player)) {
            if (!level().isClientSide) {
                doPlayerRide(player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) {
                level().addParticle(random.nextInt(3) == 0 ? ErrethysFx.PALE_DUST : ErrethysFx.SKY_DUST,
                        getRandomX(.9), getY() + random.nextDouble() * getBbHeight(), getRandomZ(.9), 0, .01, 0);
            }
            return;
        }
        if (--lifeTicks <= 0) {
            dissolve();
        }
    }

    /** Sets its riders down and comes apart into motes of mana. */
    public void dissolve() {
        if (!level().isClientSide) {
            ejectPassengers();
            ErrethysFx.dissolve(level(), getBoundingBox().getCenter(), 2.5f);
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            dissolve();
        }
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (getOwnerUUID() != null && (getOwnerUUID().equals(other.getUUID())
                || other instanceof OwnedSummon summon && getOwnerUUID().equals(summon.getOwnerUUID()))) {
            return true;
        }
        return super.isAlliedTo(other);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = tag.getInt("LifeTicks");
    }
}
