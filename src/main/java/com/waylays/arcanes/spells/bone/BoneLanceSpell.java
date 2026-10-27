package com.waylays.arcanes.spells.bone;

import com.waylays.arcanes.entity.bone.BoneLance;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Bone Lance: a long bone spear grows out of the caster's hand and is hurled, running through up to three enemies. */
@AutoSpellConfig
public class BoneLanceSpell extends BoneSpell {
    public static final float SPEED = 2.2f;

    public BoneLanceSpell() {
        super("bone_lance", SpellRarity.COMMON, 10, 3, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.pierces", BoneLance.PIERCE));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 look = entity.getLookAngle();
        Vec3 from = entity.getEyePosition().add(look.scale(.8)).subtract(0, .2, 0);
        BoneLance.throwBone(level, entity, this, BoneLance.Kind.LANCE, from, look.scale(SPEED), getPower(spellLevel, entity));
        BoneFx.crack(level, from, .8f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
