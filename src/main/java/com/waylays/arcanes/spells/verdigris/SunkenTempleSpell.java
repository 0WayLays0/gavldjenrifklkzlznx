package com.waylays.arcanes.spells.verdigris;

import com.waylays.arcanes.entity.verdigris.SunkenTemple;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Sunken Temple (ultimate): the ruins of an ancient temple rise around the enemy the caster looks at: a ring of mossy
 * copper columns that shut it in while rubble rains, then topple inward all at once.
 */
@AutoSpellConfig
public class SunkenTempleSpell extends VerdigrisSpell {
    public static final float RANGE = 24;

    public SunkenTempleSpell() {
        super("sunken_temple", SpellRarity.LEGENDARY, 3, 120, CastType.LONG, null);
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
                Component.translatable("ui.arcanes_oublies.temple_collapse", Utils.stringTruncation(power * 2.5f, 1)),
                Component.translatable("ui.arcanes_oublies.temple_rubble", Utils.stringTruncation(power * .2f, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(SunkenTemple.RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BELL_RESONATE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ANVIL_LAND);
    }

    /** While it is cast, the ground rumbles around the caster and rust and moss rise from it. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .4f;
        for (int i = 0; i < 3; i++) {
            float b = a + i * Mth.TWO_PI / 3;
            SpellUtil.particles(level, i == 0 ? VerdigrisFx.RUST_DUST : VerdigrisFx.VERDIGRIS_DUST,
                    entity.getX() + Mth.cos(b) * 1.6, entity.getY() + .1 + (entity.tickCount % 15) * .1, entity.getZ() + Mth.sin(b) * 1.6, 1, 0, 0, 0, 0);
        }
        if (entity.tickCount % 6 == 0) {
            VerdigrisFx.rise(level, entity.position(), .5f);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = aimedSpot(level, entity, RANGE);
        SunkenTemple.raise(level, entity, this, at, getPower(spellLevel, entity));
        SpellUtil.cameraShake(level, at, 30, 24);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
