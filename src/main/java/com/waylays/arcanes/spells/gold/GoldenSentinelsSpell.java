package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GoldenSentinel;
import com.waylays.arcanes.registry.ModEntities;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Golden Sentinels: golden knights rise from a shower of gold around the caster and fight for them for a while.
 * Casting the spell again sends them away (see {@link SummoningSpell}).
 */
@AutoSpellConfig
public class GoldenSentinelsSpell extends SummoningSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("golden_sentinels");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(ModSchools.GOLD_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(90)
            .build();

    public GoldenSentinelsSpell() {
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 40;
        this.baseManaCost = 120;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.summon_count", getCount(spellLevel)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getLifetime(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getAttackDamage(spellLevel, caster), 1))
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
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ARMOR_EQUIP_GOLD);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float angle = entity.tickCount * .4f;
        for (int i = 0; i < getCount(spellLevel); i++) {
            float a = angle + i * Mth.TWO_PI / getCount(spellLevel);
            SpellUtil.particles(level, GoldFx.GOLD_DUST, entity.getX() + Mth.cos(a) * 2, entity.getY() + .2, entity.getZ() + Mth.sin(a) * 2, 2, .1, .1, .1, .02);
        }
    }

    @Override
    protected void summon(Level level, int spellLevel, LivingEntity entity) {
        int count = getCount(spellLevel);
        for (int i = 0; i < count; i++) {
            float a = entity.getYRot() * Mth.DEG_TO_RAD + i * Mth.TWO_PI / count;
            Vec3 spot = SpellUtil.groundAt(level, entity.position().add(-Mth.sin(a) * 2, 0, Mth.cos(a) * 2), 3, 2);
            GoldenSentinel sentinel = ModEntities.GOLDEN_SENTINEL.get().create(level);
            if (sentinel == null) {
                continue;
            }
            sentinel.moveTo(spot.x, spot.y, spot.z, entity.getYRot(), 0);
            if (!level.noCollision(sentinel)) {
                // No room there (wall, low ceiling): rise next to the caster instead
                sentinel.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), 0);
            }
            sentinel.setup(entity, getLifetime(spellLevel), getHealth(spellLevel), getAttackDamage(spellLevel, entity));
            if (entity instanceof Mob mob && mob.getTarget() != null) {
                sentinel.setTarget(mob.getTarget());
            }
            level.addFreshEntity(sentinel);
            SpellUtil.particles(level, GoldFx.sparks(), spot.x, spot.y + .1, spot.z, 15, .3, .05, .3, .3);
            SpellUtil.particles(level, GoldFx.DEEP_DUST, spot.x, spot.y + 1, spot.z, 30, .35, .9, .35, .02);
            SpellUtil.particles(level, GoldFx.nuggetShards(), spot.x, spot.y + 2, spot.z, 12, .3, .2, .3, .1);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, .6f, 1.2f + i * .1f);
        }
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.5f, .8f);
    }

    public int getCount(int spellLevel) {
        return 2 + (spellLevel >= 3 ? 1 : 0) + (spellLevel >= 5 ? 1 : 0);
    }

    /** 30 s at level 1, +5 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 600 + (spellLevel - 1) * 100;
    }

    public double getHealth(int spellLevel) {
        return 20 + spellLevel * 4;
    }

    public float getAttackDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }
}
