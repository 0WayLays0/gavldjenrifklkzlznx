package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.Siege;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * The Siege: the caster raises a hand for a second and a half, then for five seconds blazing blocks of masonry come
 * hurtling out of the sky onto the zone looked at, half of them at the enemies in it (see Siege).
 */
@AutoSpellConfig
public class SiegeSpell extends WallSpell {
    public static final float RANGE = 30;
    public static final float RADIUS = 9;
    public static final int DURATION = 100;

    public SiegeSpell() {
        super("siege", SpellRarity.LEGENDARY, 3, 90, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
        this.castTime = 30;
        this.baseManaCost = 220;
    }

    public static int boulders(int spellLevel) {
        return 12 + 4 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.siege_boulders", boulders(spellLevel), Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(DURATION, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BELL_BLOCK);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.RAID_HORN.value());
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = SpellUtil.groundAt(level, aimedSpot(level, entity, RANGE), 6, 1);
        Siege.lay(level, entity, this, at, RADIUS, boulders(spellLevel), DURATION, getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
