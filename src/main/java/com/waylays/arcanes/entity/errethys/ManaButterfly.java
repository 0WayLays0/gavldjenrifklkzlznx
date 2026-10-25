package com.waylays.arcanes.entity.errethys;

import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.errethys.ErrethysFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A mana butterfly: it never fights. It flutters around its summoner's shoulders and, every ten seconds, pours a
 * thread of light into them that heals three hearts. It heals its summoner only.
 */
public class ManaButterfly extends ManaSummon implements FlyingAnimal {
    /** Ticks between two healings, and how much each heals (three hearts). */
    public static final int HEAL_EVERY = 200;
    public static final float HEAL = 6;

    public ManaButterfly(EntityType<? extends ManaButterfly> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5)
                .add(Attributes.FLYING_SPEED, .6)
                .add(Attributes.MOVEMENT_SPEED, .25)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.MANA_BUTTERFLY.get();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    /** No attack: it only keeps close to its summoner. */
    @Override
    protected void registerMoveGoals() {
        this.goalSelector.addGoal(1, new FlutterAroundOwnerGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) {
                level().addParticle(ErrethysFx.PALE_DUST, getRandomX(.6), getY() + random.nextDouble() * .3, getRandomZ(.6), 0, -.02, 0);
            }
            return;
        }
        // The target the summoner picks up is of no use to it
        setTarget(null);
        if (tickCount % HEAL_EVERY == 0) {
            heal(getSummoner());
        }
    }

    /** A thread of light from its wings into its summoner, who is healed. */
    private void heal(LivingEntity owner) {
        if (owner == null || !owner.isAlive()) {
            return;
        }
        Vec3 from = getBoundingBox().getCenter();
        Vec3 to = owner.getBoundingBox().getCenter();
        for (int i = 0; i <= 10; i++) {
            Vec3 p = from.lerp(to, i / 10.0);
            SpellUtil.particles(level(), i % 2 == 0 ? ErrethysFx.PALE_DUST : ErrethysFx.SKY_DUST, p.x, p.y, p.z, 1, .03, .03, .03, 0);
        }
        SpellUtil.particles(level(), ParticleTypes.HEART, to.x, to.y + owner.getBbHeight() * .4, to.z, 3, .3, .2, .3, .02);
        owner.heal(HEAL);
        level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, .8f, 1.6f);
    }

    /** Faster than a running player, as the crow. */
    @Override
    protected float getFlyingSpeed() {
        return getSpeed() * .04f;
    }

    @Override
    public boolean isFlying() {
        return !onGround();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected float getSoundVolume() {
        return .4f;
    }

    /** Loops in a lazy figure of eight round the summoner's shoulders; comes back at once when left far behind. */
    static class FlutterAroundOwnerGoal extends Goal {
        private final ManaButterfly butterfly;
        private float angle;

        FlutterAroundOwnerGoal(ManaButterfly butterfly) {
            this.butterfly = butterfly;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return butterfly.getSummoner() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity owner = butterfly.getSummoner();
            if (owner == null) {
                return;
            }
            if (butterfly.distanceToSqr(owner) > 12 * 12) {
                Vec3 spot = owner.getEyePosition().add(0, .4, 0);
                if (butterfly.level().noCollision(butterfly, butterfly.getBoundingBox().move(spot.subtract(butterfly.position())))) {
                    butterfly.moveTo(spot.x, spot.y, spot.z, butterfly.getYRot(), 0);
                }
                return;
            }
            angle += .06f;
            double x = owner.getX() + Mth.cos(angle) * 1.3;
            double z = owner.getZ() + Mth.sin(angle) * 1.3;
            double y = owner.getY() + owner.getBbHeight() * .85 + Mth.sin(angle * 2) * .35;
            butterfly.getMoveControl().setWantedPosition(x, y, z, .8);
        }
    }
}
