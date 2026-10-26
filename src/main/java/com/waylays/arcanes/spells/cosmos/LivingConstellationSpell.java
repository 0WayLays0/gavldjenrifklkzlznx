package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.cosmos.StarLion;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Living Constellation: the caster raises a hand to the sky and the stars of Leo come down one by one and join into a
 * great lion of starlight that fights at their side: it swipes with its claws and leaps on prey further off, landing
 * in a burst of stars. Cast again to send it back to the sky.
 */
@AutoSpellConfig
public class LivingConstellationSpell extends SummoningSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("living_constellation");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.COSMOS_RESOURCE)
            .setMaxLevel(6)
            .setCooldownSeconds(45)
            .build();

    public LivingConstellationSpell() {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 20;
        this.baseManaCost = 70;
    }

    public static double health(int spellLevel) {
        return 24 + 6 * spellLevel;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 1.2f;
    }

    @Override
    public int getLifetime(int spellLevel) {
        return 600 + 100 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getLifetime(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.lion_leap", Utils.stringTruncation(getDamage(spellLevel, caster) * 1.6f, 1)),
                Component.translatable("ui.irons_spellbooks.hp", (int) health(spellLevel)));
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
        return Optional.of(SoundEvents.AMETHYST_BLOCK_RESONATE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.BEACON_POWER_SELECT);
    }

    /** While the caster chants, stars kindle above them one after another. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (entity.tickCount % 2 == 0) {
            float a = entity.tickCount * .6f;
            Vec3 at = entity.position().add(Mth.cos(a) * 1.5, entity.getBbHeight() + 1.5 + Mth.sin(a * .7f) * .5, Mth.sin(a) * 1.5);
            CosmosFx.stars(level, at, 3, .05);
        }
    }

    /** The lion takes shape a few steps in front of the caster, from stars coming down from the sky. */
    @Override
    protected boolean summon(Level level, int spellLevel, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            dismissSummons(serverLevel, entity);
        }
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        Vec3 spot = SpellUtil.groundAt(level, entity.position().add(-Mth.sin(yaw) * 2.5, 0, Mth.cos(yaw) * 2.5), 3, 2);
        StarLion lion = ModEntities.STAR_LION.get().create(level);
        if (lion == null) {
            return false;
        }
        lion.moveTo(spot.x, spot.y, spot.z, entity.getYRot(), 0);
        if (!level.noCollision(lion)) {
            lion.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), 0);
        }
        lion.setYHeadRot(entity.getYRot());
        lion.yBodyRot = entity.getYRot();
        lion.setup(entity, getLifetime(spellLevel), health(spellLevel), getDamage(spellLevel, entity));
        if (entity instanceof Mob mob && mob.getTarget() != null) {
            lion.setTarget(mob.getTarget());
        }
        level.addFreshEntity(lion);
        CosmosFx.hum(level, spot, 1.4f, .8f);
        CosmosFx.sound(level, spot, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, .6f);
        return true;
    }
}
