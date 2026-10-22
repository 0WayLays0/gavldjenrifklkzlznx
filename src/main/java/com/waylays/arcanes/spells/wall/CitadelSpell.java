package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.Citadel;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Tyrant's Fortress (ultimate): a fortress rises around the caster, its round wall built brick by brick and eight
 * towers bursting up along it; for ten seconds the towers shell every enemy around while the caster stands unshaken
 * behind them; then the whole fortress blows apart outward.
 */
@AutoSpellConfig
public class CitadelSpell extends WallSpell {
    public CitadelSpell() {
        super("fallen_citadel", SpellRarity.LEGENDARY, 3, 150, CastType.LONG, null);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 9;
        this.spellPowerPerLevel = 2;
        this.castTime = 40;
        this.baseManaCost = 250;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.tower_shells", Utils.stringTruncation(power * .6f, 1)),
                Component.translatable("ui.arcanes_oublies.fortress_blast", Utils.stringTruncation(power * 2, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(Citadel.SHELL_RANGE, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(Citadel.HOLD, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.STONE_BREAK);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ANVIL_LAND);
    }

    /** While it is cast, the sigil spreads under the caster, the ground shakes and embers rise. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (entity.tickCount % 10 == 0) {
            WallFx.sigil(level, SpellUtil.groundAt(level, entity.position(), 2, 1), 4 + (entity.tickCount % 40) / 4f, 14);
            WallFx.rise(level, entity.position(), .6f);
        }
        if (entity.tickCount % 3 == 0) {
            float a = entity.tickCount * .4f;
            WallFx.embers(level, entity.position().add(Mth.cos(a) * 2, .3, Mth.sin(a) * 2), 4, .2);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = SpellUtil.groundAt(level, entity.position(), 2, 1);
        Citadel.raise(level, entity, this, at, getPower(spellLevel, entity));
        SpellUtil.cameraShake(level, at, 40, 30);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
