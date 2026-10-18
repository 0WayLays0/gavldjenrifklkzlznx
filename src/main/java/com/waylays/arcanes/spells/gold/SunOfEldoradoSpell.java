package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.EldoradoSun;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Sun of Eldorado (ultimate): a golden sun rises above the caster, fires beams at every nearby enemy, then collapses
 * into a huge golden explosion.
 */
@AutoSpellConfig
public class SunOfEldoradoSpell extends AbstractSpell {
    public static final float RANGE = 16f;
    public static final float EXPLOSION_RADIUS = 7f;
    public static final double HEIGHT = 5.5;

    private final ResourceLocation spellId = ArcanesOublies.id("sun_of_eldorado");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(ModSchools.GOLD_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(120)
            .build();

    public SunOfEldoradoSpell() {
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 4;
        this.castTime = 50;
        this.baseManaCost = 250;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.beam_damage", Utils.stringTruncation(getBeamDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getExplosionDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(EXPLOSION_RADIUS, 1))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_AMBIENT);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.BELL_RESONATE);
    }

    /** While charging: a column of gold rises from the caster into the sky. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .5f;
        for (int i = 0; i < 3; i++) {
            float b = a + i * Mth.TWO_PI / 3;
            SpellUtil.particles(level, GoldFx.GOLD_DUST, entity.getX() + Mth.cos(b) * .8, entity.getY() + 2 + (entity.tickCount % 20) * .2, entity.getZ() + Mth.sin(b) * .8, 1, 0, 0, 0, 0);
        }
        SpellUtil.particles(level, GoldFx.sparks(), entity.getX(), entity.getY() + HEIGHT, entity.getZ(), 2, .3, .3, .3, .2);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 feet = entity.position();
        double height = HEIGHT;
        HitResult roof = level.clip(new ClipContext(feet.add(0, 2, 0), feet.add(0, HEIGHT + 1.5, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (roof.getType() != HitResult.Type.MISS) {
            height = Math.max(2.2, roof.getLocation().y - feet.y - 1.5);
        }
        EldoradoSun sun = new EldoradoSun(level, entity, getBeamDamage(spellLevel, entity), getExplosionDamage(spellLevel, entity));
        sun.moveTo(feet.x, feet.y + height, feet.z);
        level.addFreshEntity(sun);
        GoldFx.particleLine(level, entity.getEyePosition(), sun.center(), .3);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getBeamDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .5f;
    }

    public float getExplosionDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 2f;
    }
}
