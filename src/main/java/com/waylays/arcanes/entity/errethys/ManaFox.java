package com.waylays.arcanes.entity.errethys;

import com.waylays.arcanes.entity.SummonFollowOwnerGoal;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.errethys.ErrethysFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A mana fox: lithe and very fast, it darts after what its summoner fights and its bite leaves the prey bleeding.
 */
public class ManaFox extends ManaSummon {
    /** How long a bite bleeds. */
    public static final int BLEED_TICKS = 80;

    public ManaFox(EntityType<? extends ManaFox> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8)
                .add(Attributes.MOVEMENT_SPEED, .42)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.MANA_FOX.get();
    }

    @Override
    protected void registerMoveGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.6, true));
        this.goalSelector.addGoal(2, new SummonFollowOwnerGoal(this, 1.5, 5, 2, -1));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    /** Its bite opens a wound that bleeds for a while. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity prey) {
            prey.addEffect(new MobEffectInstance(ModEffects.BLEEDING, BLEED_TICKS, 0, false, true, true));
            SpellUtil.particles(level(), ErrethysFx.PALE_DUST, prey.getX(), prey.getY() + prey.getBbHeight() * .6, prey.getZ(), 6, .2, .2, .2, .02);
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    public float getVoicePitch() {
        return 1.2f + random.nextFloat() * .2f;
    }

    @Override
    protected float getSoundVolume() {
        return .6f;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.FOX_SNIFF, .1f, 1.4f);
    }
}
