package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.GemCrown;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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

/** The Crown of the Five Gems (ultimate): see {@link GemCrown}. */
@AutoSpellConfig
public class CrownOfGemsSpell extends GemSpell {
    public static final float RANGE = 32;

    public CrownOfGemsSpell() {
        super("crown_of_gems", SpellRarity.LEGENDARY, 3, 150, CastType.LONG, null);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 50;
        this.baseManaCost = 250;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.crown_gems", Utils.timeFromTicks(GemCrown.ACT, 1)),
                Component.translatable("ui.arcanes_oublies.crown_strike", Utils.stringTruncation(power * 3, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(GemCrown.BLAST, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_RESONATE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    /** While it is cast, the pentagram draws itself on the ground in the five colours, one after another. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (entity.tickCount % 8 == 0) {
            Gem gem = Gem.of((entity.tickCount / 8) % 5);
            GemFx.sigil(level, SpellUtil.groundAt(level, entity.position(), 2, 1), gem, 3 + gem.ordinal() * .6f, 16);
            GemFx.chime(level, entity.position(), .7f, .7f + .2f * gem.ordinal());
        }
        if (entity.tickCount % 3 == 0) {
            Gem gem = Gem.of(entity.getRandom().nextInt(5));
            float a = entity.tickCount * .5f;
            GemFx.glitter(level, entity.position().add(Mth.cos(a) * 2.5, .3, Mth.sin(a) * 2.5), gem, 3, .2);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = SpellUtil.groundAt(level, entity.position(), 2, 1);
        Vec3 strike = SpellUtil.aimedSpot(level, entity, RANGE);
        GemCrown.raise(level, entity, this, at, strike, getPower(spellLevel, entity));
        SpellUtil.cameraShake(level, at, 30, 24);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
