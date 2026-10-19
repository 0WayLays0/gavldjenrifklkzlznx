package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.errethys.ManaCamel;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Mana Dromedary: a saddled dromedary of mana takes shape beside the caster, who can ride it (see
 * {@link ManaCamel}). Casting the spell again sends it away (see {@link SummoningSpell}).
 */
@AutoSpellConfig
public class ManaDromedarySpell extends SummoningSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("mana_dromedary");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.ERRETHYS_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(60)
            .build();

    public ManaDromedarySpell() {
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 30;
        this.baseManaCost = 80;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getLifetime(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.hp", (int) getHealth(spellLevel))
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
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.CAMEL_SADDLE);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        // Sand of mana swirling up where the dromedary will stand
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        Vec3 spot = entity.position().add(-Mth.sin(yaw) * 2.5, 0, Mth.cos(yaw) * 2.5);
        float a = entity.tickCount * .4f;
        SpellUtil.particles(level, ErrethysFx.SKY_DUST, spot.x + Mth.cos(a) * 1.2, spot.y + .2 + (entity.tickCount % 30) / 15.0, spot.z + Mth.sin(a) * 1.2, 2, .05, .05, .05, 0);
        SpellUtil.particles(level, ErrethysFx.PALE_DUST, spot.x - Mth.cos(a) * 1.2, spot.y + .2, spot.z - Mth.sin(a) * 1.2, 1, .05, .05, .05, 0);
    }

    /** One dromedary per summoner: a new one replaces the one still around (from a scroll, say). */
    @Override
    protected void summon(Level level, int spellLevel, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            dismissSummons(serverLevel, entity);
        }
        ManaCamel camel = ModEntities.MANA_DROMEDARY.get().create(level);
        if (camel != null) {
            float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
            Vec3 spot = SpellUtil.groundAt(level, entity.position().add(-Mth.sin(yaw) * 2.5, 0, Mth.cos(yaw) * 2.5), 3, 3);
            camel.moveTo(spot.x, spot.y, spot.z, entity.getYRot(), 0);
            if (!level.noCollision(camel)) {
                camel.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), 0);
            }
            camel.setup(entity, getLifetime(spellLevel), getHealth(spellLevel));
            level.addFreshEntity(camel);
            ErrethysFx.materialize(level, camel.position(), 2.2f);
        }
    }

    /** 2 min at level 1, +1 min per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 1200 + spellLevel * 1200;
    }

    public double getHealth(int spellLevel) {
        return 30 + spellLevel * 10;
    }
}
