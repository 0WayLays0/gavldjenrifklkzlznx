package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GoldSpikeEntity;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
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
 * Golden Spikes: a wave of gold crystal clusters bursts out of the ground in a line in front of the caster, each one
 * hurting and tossing what it strikes, and the wave breaks in a crown of spikes at its end (see {@link GoldSpikeEntity}).
 */
@AutoSpellConfig
public class GoldenSpikesSpell extends AbstractSpell {
    public static final double SPACING = 1.25;
    public static final int TICKS_BETWEEN_SPIKES = 2;

    private final ResourceLocation spellId = ArcanesOublies.id("golden_spikes");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.GOLD_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(8)
            .build();

    public GoldenSpikesSpell() {
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.arcanes_oublies.length", Utils.stringTruncation(getSpikeCount(spellLevel) * SPACING, 1))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 look = entity.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        forward = forward.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, entity.getYRot()) : forward.normalize();
        float damage = getDamage(spellLevel, entity);
        int count = getSpikeCount(spellLevel);
        for (int i = 0; i < count; i++) {
            // The wave wavers a little from side to side
            Vec3 side = new Vec3(-forward.z, 0, forward.x).scale(Mth.nextDouble(entity.getRandom(), -.35, .35));
            Vec3 at = entity.position().add(forward.scale(1.5 + i * SPACING)).add(side);
            Vec3 ground = SpellUtil.groundAt(level, at, 4, 1);
            GoldSpikeEntity spike = new GoldSpikeEntity(level, entity, damage, 2 + i * TICKS_BETWEEN_SPIKES);
            spike.moveTo(ground.x, ground.y, ground.z, entity.getRandom().nextFloat() * 360, 0);
            spike.setCrown(i == count - 1);
            level.addFreshEntity(spike);
        }
        GoldFx.sigil(level, entity.position(), 1.4f, 20);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public int getSpikeCount(int spellLevel) {
        return 6 + spellLevel;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }
}
