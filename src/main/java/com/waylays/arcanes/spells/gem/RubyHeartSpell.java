package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.RubyHeart;
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

/** Burning Heart: a ruby thrown spinning that sets itself in its target, beats three times and bursts (see {@link RubyHeart}). */
@AutoSpellConfig
public class RubyHeartSpell extends GemSpell {
    public RubyHeartSpell() {
        super("ruby_heart", SpellRarity.UNCOMMON, 8, 10, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 9;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.heart_beats", RubyHeart.BEATS, Utils.stringTruncation(power * .3f, 1)),
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RubyHeart.BLAST_RADIUS, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 look = entity.getLookAngle();
        Vec3 from = entity.getEyePosition().add(look.scale(.8)).add(0, -.2, 0);
        RubyHeart.hurl(level, entity, this, from, look.scale(1.25).add(0, .1, 0), getPower(spellLevel, entity));
        GemFx.sigil(level, SpellUtil.groundAt(level, entity.position(), 2, 1), Gem.RUBY, 1.6f, 18);
        GemFx.sound(level, from, net.minecraft.sounds.SoundEvents.BLAZE_SHOOT, .8f, 1.3f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
