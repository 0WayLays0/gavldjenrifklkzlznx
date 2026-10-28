package com.waylays.arcanes.spells.wood;

import com.waylays.arcanes.entity.wood.VineLash;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Vine Lash: a thorny vine shoots from the caster's hand: it catches the enemy looked at and yanks it to them, or hooks
 * the block looked at and hauls them to it (see VineLash).
 */
@AutoSpellConfig
public class VineLashSpell extends WoodSpell {
    public static final double RANGE = 22;
    public static final double MISS = 12;

    public VineLashSpell() {
        super("vine_lash", SpellRarity.UNCOMMON, 8, 8, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.cast_range", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float damage = getPower(spellLevel, entity);
        LivingEntity target = aimed(level, entity, RANGE);
        Vec3 eye = entity.getEyePosition();
        if (target != null) {
            VineLash.lash(level, entity, this, VineLash.Mode.ENEMY, target, target.position(), damage);
        } else {
            BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(entity.getLookAngle().scale(RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            if (hit.getType() == HitResult.Type.BLOCK) {
                VineLash.lash(level, entity, this, VineLash.Mode.GRAPPLE, null, hit.getLocation(), damage);
            } else {
                VineLash.lash(level, entity, this, VineLash.Mode.MISS, null, eye.add(entity.getLookAngle().scale(MISS)), damage);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
