package com.waylays.arcanes.entity;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import javax.annotation.Nullable;
import java.util.EnumSet;

/** A summon targets what attacked its owner ({@code defend}) or what its owner attacked. */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class OwnerTargetGoal extends TargetGoal {
    private final OwnedSummon summon;
    private final boolean defend;
    @Nullable
    private LivingEntity candidate;
    private int lastTimestamp;

    public <T extends Mob & OwnedSummon> OwnerTargetGoal(T summon, boolean defend) {
        super(summon, false);
        this.summon = summon;
        this.defend = defend;
        setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = summon.getSummoner();
        if (owner == null) {
            return false;
        }
        candidate = defend ? owner.getLastHurtByMob() : owner.getLastHurtMob();
        int timestamp = defend ? owner.getLastHurtByMobTimestamp() : owner.getLastHurtMobTimestamp();
        return candidate != null && timestamp != lastTimestamp && !SpellUtil.isAlly(owner, candidate)
                && canAttack(candidate, TargetingConditions.DEFAULT);
    }

    /**
     * The game only remembers what a creature struck in melee or with a shot: remember what its spells (and whatever
     * else it does damage with) struck too, so that its summons go after it.
     */
    @SubscribeEvent
    public static void rememberStruck(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (!victim.level().isClientSide && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != victim) {
            attacker.setLastHurtMob(victim);
        }
    }

    @Override
    public void start() {
        LivingEntity owner = summon.getSummoner();
        if (owner != null) {
            lastTimestamp = defend ? owner.getLastHurtByMobTimestamp() : owner.getLastHurtMobTimestamp();
        }
        mob.setTarget(candidate);
        super.start();
    }
}
