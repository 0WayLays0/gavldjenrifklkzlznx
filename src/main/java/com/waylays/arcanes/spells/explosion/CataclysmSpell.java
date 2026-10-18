package com.waylays.arcanes.spells.explosion;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.CataclysmCore;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Cataclysm (ultimate): the caster rises into the air while a sphere of energy swells above them for three seconds,
 * then explodes in a giant dome of white light with a crown of secondary explosions all around
 * (see {@link CataclysmCore}).
 */
@AutoSpellConfig
public class CataclysmSpell extends AbstractSpell {
    public static final float RADIUS = 11f;
    public static final float CROWN_BLAST_RADIUS = 3f;
    public static final double HEIGHT = 8;

    private final ResourceLocation spellId = ArcanesOublies.id("cataclysm");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(ModSchools.EXPLOSION_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(150)
            .build();

    public CataclysmSpell() {
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 14;
        this.spellPowerPerLevel = 4;
        this.castTime = 30;
        this.baseManaCost = 300;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1))
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
        return Optional.of(SoundEvents.BEACON_POWER_SELECT);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    /** While charging: embers spiral up around the caster. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .45f;
        for (int i = 0; i < 4; i++) {
            float b = a + i * Mth.HALF_PI;
            double y = entity.getY() + (entity.tickCount % 15) * .18;
            SpellUtil.particles(level, i % 2 == 0 ? ExplosionFx.FIRE_DUST : ExplosionFx.ARCANE_DUST, entity.getX() + Mth.cos(b) * 1.2, y, entity.getZ() + Mth.sin(b) * 1.2, 1, 0, 0, 0, 0);
        }
        SpellUtil.particles(level, ExplosionFx.sparks(), entity.getX(), entity.getY() + .1, entity.getZ(), 2, .6, 0, .6, .15);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 feet = entity.position();
        double groundY = SpellUtil.groundAt(level, feet, 6, 1).y;
        // The caster rises, then hovers down slowly
        entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1, false, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, false, false, true));
        double height = HEIGHT;
        HitResult roof = level.clip(new ClipContext(feet.add(0, 2, 0), feet.add(0, HEIGHT + 2, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (roof.getType() != HitResult.Type.MISS) {
            height = Math.max(3, roof.getLocation().y - feet.y - 2);
        }
        CataclysmCore core = new CataclysmCore(level, entity, getDamage(spellLevel, entity), groundY);
        core.moveTo(feet.x, feet.y + height - core.getBbHeight() * .5, feet.z);
        level.addFreshEntity(core);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 2.2f;
    }
}
