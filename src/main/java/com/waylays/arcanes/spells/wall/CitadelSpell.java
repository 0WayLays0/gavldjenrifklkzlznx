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
 * The Fallen Citadel (ultimate): a whole fortress of stone bricks rises around the enemy looked at, shuts it in while
 * its towers lob bricks down on it, then every wall topples inward at once.
 */
@AutoSpellConfig
public class CitadelSpell extends WallSpell {
    public static final float RANGE = 24;

    public CitadelSpell() {
        super("fallen_citadel", SpellRarity.LEGENDARY, 3, 120, CastType.LONG, null);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 30;
        this.baseManaCost = 250;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.citadel_collapse", Utils.stringTruncation(power * 3, 1)),
                Component.translatable("ui.arcanes_oublies.tower_bricks", Utils.stringTruncation(power * .3f, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(Citadel.RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.STONE_BREAK);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ANVIL_LAND);
    }

    /** While it is cast, the ground rumbles and brick dust rises around the caster. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (entity.tickCount % 4 == 0) {
            float a = entity.tickCount * .3f;
            WallFx.dust(level, entity.position().add(Mth.cos(a) * 1.6, .2, Mth.sin(a) * 1.6), 6, .3);
        }
        if (entity.tickCount % 8 == 0) {
            WallFx.rise(level, entity.position(), .4f);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = aimedSpot(level, entity, RANGE);
        Citadel.raise(level, entity, this, at, getPower(spellLevel, entity));
        SpellUtil.cameraShake(level, at, 30, 24);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
