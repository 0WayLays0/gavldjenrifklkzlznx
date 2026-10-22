package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.Rampart;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Oubliette: a closed cell of real stone bricks builds itself round the enemy looked at, ring after ring then the lid:
 * it is walled in, in the dark, until the cell crumbles.
 */
@AutoSpellConfig
public class BrickCageSpell extends WallSpell {
    public static final int RANGE = 20;

    public BrickCageSpell() {
        super("brick_cage", SpellRarity.RARE, 5, 22, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    public static int duration(int spellLevel) {
        return 80 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.walled_in", Utils.timeFromTicks(duration(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, RANGE, .35f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            LivingEntity target = targetData.getTarget((ServerLevel) level);
            if (target != null) {
                cage(level, entity, target, duration(spellLevel), getPower(spellLevel, entity));
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public void cage(Level level, LivingEntity caster, LivingEntity target, int life, float damage) {
        // The cell rises round the block the target stands in, wide and high enough to hold it: the target is set in
        // the middle of it first, so the walls close round it and do not push it out
        int half = Mth.clamp(Mth.ceil((target.getBbWidth() + 1) * .5f), 1, 3);
        int height = Mth.clamp(Mth.ceil(target.getBbHeight() + .2f), 2, 6);
        BlockPos cell = BlockPos.containing(target.position());
        target.teleportTo(cell.getX() + .5, target.getY(), cell.getZ() + .5);
        Rampart.raise(level, caster, Rampart.Shape.CAGE, target.position(), 0, half, height, life);
        target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
        target.hurtMarked = true;
        target.invulnerableTime = 0;
        io.redspace.ironsspellbooks.damage.DamageSources.applyDamage(target, damage, SpellUtil.damageSource(this, caster, caster));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, life, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
    }
}
