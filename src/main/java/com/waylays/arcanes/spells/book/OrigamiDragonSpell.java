package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.OrigamiDragon;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.SummoningSpell;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
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
 * Origami Dragon (ultimate): the caster's pages rise in a helix and gather into one great sheet above them, which folds
 * itself into a paper dragon that hunts their enemies for a while (see {@link OrigamiDragon}). Casting the spell again
 * sends it away (see {@link SummoningSpell}).
 */
@AutoSpellConfig
public class OrigamiDragonSpell extends SummoningSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("origami_dragon");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(150)
            .build();

    public OrigamiDragonSpell() {
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 4;
        this.castTime = 60;
        this.baseManaCost = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getSpellPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.paper_breath", Utils.stringTruncation(power * .3f, 1)),
                Component.translatable("ui.arcanes_oublies.dive_damage", Utils.stringTruncation(power * 1.5f, 1)),
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
        return Optional.of(SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.EVOKER_CAST_SPELL);
    }

    /** While chanting, pages rise around the caster in a widening helix up to where the sheet will form. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .4f;
        for (int i = 0; i < 3; i++) {
            float b = a + i * Mth.TWO_PI / 3;
            double h = ((entity.tickCount + i * 7) % 20) / 20.0;
            double r = .8 + h * 1.6;
            SpellUtil.particles(level, BookFx.paper(), entity.getX() + Mth.cos(b) * r, entity.getY() + .2 + h * 3.5, entity.getZ() + Mth.sin(b) * r, 1, .02, .02, .02, 0);
            SpellUtil.particles(level, ParticleTypes.ENCHANT, entity.getX() + Mth.cos(b + .5f) * r, entity.getY() + .4 + h * 3.5, entity.getZ() + Mth.sin(b + .5f) * r, 2, .05, .05, .05, .2);
        }
        if (entity.tickCount % 10 == 0) {
            SpellUtil.particles(level, BookFx.GILT_DUST, entity.getX(), entity.getY() + 4, entity.getZ(), 10, 1, .2, 1, 0);
        }
    }

    /** One dragon per caster: a new one replaces the one still around. */
    @Override
    protected void summon(Level level, int spellLevel, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            dismissSummons(serverLevel, entity);
        }
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        Vec3 at = entity.position().add(-Mth.sin(yaw) * 2, 4, Mth.cos(yaw) * 2);
        level.addFreshEntity(new OrigamiDragon(level, entity, at, getLifetime(spellLevel), getSpellPower(spellLevel, entity)));
        SpellUtil.particles(level, BookFx.paper(), at.x, at.y, at.z, 40, 1.5, .3, 1.5, .1);
        SpellUtil.particles(level, ParticleTypes.ENCHANT, at.x, at.y, at.z, 80, 1.5, .3, 1.5, 1);
        SpellUtil.cameraShake(level, at, 10, 16);
    }

    /** 18 s at level 1, +3 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 300 + spellLevel * 60;
    }
}
