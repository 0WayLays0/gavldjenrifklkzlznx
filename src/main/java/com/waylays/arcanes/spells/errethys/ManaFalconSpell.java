package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.errethys.ManaFalcon;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Mana Falcon: a great falcon of mana takes shape under the caster and carries them off; they fly where they look,
 * fast (see {@link ManaFalcon}). Casting the spell again sends it away (see {@link SummoningSpell}).
 */
@AutoSpellConfig
public class ManaFalconSpell extends SummoningSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("mana_falcon");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.ERRETHYS_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30)
            .build();

    public ManaFalconSpell() {
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 20;
        this.baseManaCost = 60;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getLifetime(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.flight_speed", Utils.stringTruncation(getSpeed(spellLevel) * 20, 1)),
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
        return Optional.of(SoundEvents.ENDER_DRAGON_FLAP);
    }

    /** While chanting, motes of mana gather in two wide wings round the caster. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        float sweep = Mth.sin(entity.tickCount * .3f) * .5f;
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 1; i <= 3; i++) {
                double out = side * i * .7;
                double x = entity.getX() + Mth.cos(yaw) * out;
                double z = entity.getZ() + Mth.sin(yaw) * out;
                SpellUtil.particles(level, i == 3 ? ErrethysFx.PALE_DUST : ErrethysFx.SKY_DUST, x, entity.getY() + 1.2 + sweep * i * .3, z, 1, .02, .02, .02, 0);
            }
        }
    }

    /** One falcon per caster: a new one replaces the one still around. The caster climbs straight on. */
    @Override
    protected boolean summon(Level level, int spellLevel, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            dismissSummons(serverLevel, entity);
        }
        ManaFalcon falcon = ModEntities.MANA_FALCON.get().create(level);
        if (falcon == null) {
            return false;
        }
        entity.stopRiding();
        falcon.moveTo(entity.getX(), entity.getY() + .1, entity.getZ(), entity.getYRot(), 0);
        falcon.setup(entity, getLifetime(spellLevel), getHealth(spellLevel), getSpeed(spellLevel));
        level.addFreshEntity(falcon);
        entity.startRiding(falcon, true);
        // A beat of its wings lifts them off
        falcon.setDeltaMovement(0, .5, 0);
        ErrethysFx.materialize(level, falcon.position().add(0, .5, 0), 2.5f);
        return true;
    }

    /** 1 min at level 1, +30 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 600 + spellLevel * 600;
    }

    public double getHealth(int spellLevel) {
        return 15;
    }

    /** Top speed in blocks per tick: about 17 blocks per second at level 1, 22 at level 5. */
    public float getSpeed(int spellLevel) {
        return .8f + spellLevel * .06f;
    }
}
