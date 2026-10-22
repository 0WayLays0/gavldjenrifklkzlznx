package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.DiamondPrism;
import com.waylays.arcanes.spells.SpellUtil;
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

/** Diamond Prism: a great diamond over the spot aimed at, striking enemies with rainbow rays and splitting Ruby Rays (see {@link DiamondPrism}). */
@AutoSpellConfig
public class DiamondPrismSpell extends GemSpell {
    public static final float RANGE = 20;

    public DiamondPrismSpell() {
        super("diamond_prism", SpellRarity.RARE, 6, 20, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 55;
    }

    public static int duration(int spellLevel) {
        return 260 + 40 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.prism_rays", DiamondPrism.MAX_SHOTS, Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.splits_ruby_ray"),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = SpellUtil.aimedGround(level, entity, RANGE);
        DiamondPrism.place(level, entity, this, at, duration(spellLevel), getPower(spellLevel, entity));
        GemFx.sigil(level, at, Gem.DIAMOND, 2.2f, 30);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
