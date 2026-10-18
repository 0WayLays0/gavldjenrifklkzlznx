package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.InkWave;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Living Ink: a wave of living ink spreads out in front of the caster; black hands rise from it, seize and blind every
 * enemy it reaches (see {@link InkWave} and {@link com.waylays.arcanes.entity.book.InkHands}).
 */
@AutoSpellConfig
public class LivingInkSpell extends AbstractSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("living_ink");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(16)
            .build();

    public LivingInkSpell() {
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.arcanes_oublies.hold_time", Utils.timeFromTicks(getHoldTicks(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.length", Utils.stringTruncation(getLength(spellLevel), 1))
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
        return Optional.of(SoundEvents.SQUID_SQUIRT);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 look = entity.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        forward = forward.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, entity.getYRot()) : forward.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        Vec3 start = SpellUtil.groundAt(level, entity.position().add(forward.scale(.8)), 3, 1);
        InkWave wave = new InkWave(level, entity, getDamage(spellLevel, entity), getHoldTicks(spellLevel), getLength(spellLevel));
        wave.moveTo(start.x, start.y, start.z, yaw, 0);
        level.addFreshEntity(wave);
        // Ink pours from the caster's hands onto the ground
        Vec3 hands = entity.getEyePosition().add(look.scale(.7)).subtract(0, .4, 0);
        SpellUtil.particles(level, ParticleTypes.SQUID_INK, hands.x, hands.y, hands.z, 12, .15, .1, .15, .08);
        SpellUtil.particles(level, BookFx.INK_SHEEN_DUST, hands.x, hands.y, hands.z, 8, .2, .2, .2, 0);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 2 s at level 1, +0.25 s per level. */
    public int getHoldTicks(int spellLevel) {
        return 35 + spellLevel * 5;
    }

    public float getLength(int spellLevel) {
        return 8.5f + spellLevel * .5f;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }
}
