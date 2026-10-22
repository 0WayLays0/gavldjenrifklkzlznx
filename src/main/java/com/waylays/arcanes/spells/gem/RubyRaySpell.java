package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.RubyRay;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;
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

/** Ruby Ray: hold to pour a beam of burning red light out of a ruby before the hand (see {@link RubyRay}). */
@AutoSpellConfig
public class RubyRaySpell extends GemSpell {
    public RubyRaySpell() {
        super("ruby_ray", SpellRarity.COMMON, 10, 12, CastType.CONTINUOUS, null);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 80;
        this.baseManaCost = 10;
    }

    public float damage(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .5f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_per_second", Utils.stringTruncation(damage(spellLevel, caster) * 20f / RubyRay.HIT_TICKS, 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RubyRay.RANGE, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        RubyRay.hold(level, entity, this, damage(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        RubyRay.hold(level, entity, this, damage(spellLevel, entity));
        if (entity.tickCount % 10 == 0) {
            GemFx.sigil(level, SpellUtil.groundAt(level, entity.position(), 2, 1), Gem.RUBY, 1.6f, 14);
        }
    }
}
