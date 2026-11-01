package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.DragonSun;
import com.waylays.arcanes.entity.gold.GoldenDragon;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.SummoningSpell;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
 * Golden Dragon (bonus, legendary): a giant sun blazes up in the sky over the caster and the Golden Dragon of Eldorado
 * pours out of it to hunt their enemies for a while, burning them with its molten breath, coiling round
 * them and diving through them (see {@link GoldenDragon}). Casting the spell again sends it away (see
 * {@link SummoningSpell}).
 */
@AutoSpellConfig
public class GoldenDragonSpell extends SummoningSpell {
    /** Where the sun rises: high in front of the caster, where they can see it. */
    public static final double SUN_HEIGHT = 7;
    public static final double SUN_AHEAD = 6;

    private final ResourceLocation spellId = ArcanesOublies.id("golden_dragon");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(ModSchools.GOLD_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(150)
            .build();

    public GoldenDragonSpell() {
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 4;
        this.castTime = 60;
        this.baseManaCost = 220;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getSpellPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.molten_breath", Utils.stringTruncation(power * .3f, 1)),
                Component.translatable("ui.arcanes_oublies.dive_damage", Utils.stringTruncation(power * 1.4f, 1)),
                Component.translatable("ui.arcanes_oublies.dragon_coil"),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getLifetime(spellLevel), 1))
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
        return Optional.of(SoundEvents.ENDER_DRAGON_GROWL);
    }

    /** While chanting, gold dust spirals up round the caster to where the sun will rise, and the sun burns under them. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (entity.tickCount % 20 == 0) {
            GoldFx.sigil(level, entity.position(), 2.4f, 24);
        }
        float a = entity.tickCount * .35f;
        for (int i = 0; i < 3; i++) {
            float b = a + i * Mth.TWO_PI / 3;
            double h = ((entity.tickCount + i * 7) % 24) / 24.0;
            double r = .9 + h * 1.4;
            SpellUtil.particles(level, i == 0 ? GoldFx.sparks() : GoldFx.GOLD_DUST, entity.getX() + Mth.cos(b) * r, entity.getY() + .2 + h * SUN_HEIGHT,
                    entity.getZ() + Mth.sin(b) * r, 1, .02, .02, .02, 0);
        }
    }

    /** One dragon per caster: a new one replaces the one still around. */
    @Override
    protected boolean summon(Level level, int spellLevel, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            dismissSummons(serverLevel, entity);
        }
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        Vec3 at = entity.position().add(-Mth.sin(yaw) * SUN_AHEAD, SUN_HEIGHT, Mth.cos(yaw) * SUN_AHEAD);
        DragonSun.rise(level, at);
        level.addFreshEntity(new GoldenDragon(level, entity, at, getLifetime(spellLevel), getSpellPower(spellLevel, entity)));
        SpellUtil.particles(level, GoldFx.sparks(), at.x, at.y, at.z, 60, 2.5, 2.5, 2.5, .5);
        SpellUtil.cameraShake(level, at, 15, 24);
        return true;
    }

    /** 20 s at level 1, +4 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 320 + spellLevel * 80;
    }
}
