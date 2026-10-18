package com.waylays.arcanes.spells.explosion;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.ChainDetonation;
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
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Chain Detonation: an explosion hits the target, then jumps from enemy to enemy, each blast bigger than the last.
 */
@AutoSpellConfig
public class ChainDetonationSpell extends AbstractSpell {
    public static final float JUMP_RANGE = 8f;

    private final ResourceLocation spellId = ArcanesOublies.id("chain_detonation");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.EXPLOSION_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(12)
            .build();

    public ChainDetonationSpell() {
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 15;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.arcanes_oublies.jumps", getJumps(spellLevel))
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
        return Optional.of(SoundEvents.TNT_PRIMED);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.FIRECHARGE_USE);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 24, .35f);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        SpellUtil.particles(level, ExplosionFx.arcaneSparks(), entity.getX(), entity.getY() + 1.2, entity.getZ(), 2, .3, .3, .3, .15);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            LivingEntity target = targetData.getTarget((ServerLevel) level);
            if (target != null) {
                level.addFreshEntity(new ChainDetonation(level, entity, target, getDamage(spellLevel, entity), getJumps(spellLevel)));
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** Blasts in the chain, the first target included: 3 at level 1, up to 5. */
    public int getJumps(int spellLevel) {
        return Math.min(5, 3 + (spellLevel - 1) / 3);
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }
}
