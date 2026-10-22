package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GildedChainsEntity;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
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
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.util.ModTags;
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
 * Gilded Chains: golden chains burst out of the ground around the target, pin it in place and lay two coats of gold on
 * it, then one more every two seconds it stays bound. Works like Iron's Root: the target rides an immobile
 * {@link GildedChainsEntity} it cannot dismount.
 */
@AutoSpellConfig
public class GildedChainsSpell extends AbstractSpell {
    /** Coats of gold the chains lay as they bind. */
    public static final int GILDS = 2;

    private final ResourceLocation spellId = ArcanesOublies.id("gilded_chains");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GOLD_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(20)
            .build();

    public GildedChainsSpell() {
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 20;
        this.baseManaCost = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(getDuration(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.arcanes_oublies.gilds", GILDS),
                Component.translatable("ui.arcanes_oublies.gild_every", Utils.timeFromTicks(GildedChainsEntity.GILD_EVERY, 1))
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
        return Optional.of(SoundEvents.CHAIN_PLACE);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 32, .35f);
    }

    /** While charging: golden glitter spirals around the caster's hands. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float angle = entity.tickCount * .6f;
        double radius = .6;
        double x = entity.getX() + Mth.cos(angle) * radius;
        double z = entity.getZ() + Mth.sin(angle) * radius;
        double y = entity.getY() + 1.1 + Mth.sin(angle * .5f) * .2;
        SpellUtil.particles(level, GoldFx.GOLD_DUST, x, y, z, 2, .03, .03, .03, 0);
        SpellUtil.particles(level, GoldFx.PALE_DUST, entity.getX() - Mth.cos(angle) * radius, y, entity.getZ() - Mth.sin(angle) * radius, 1, .03, .03, .03, 0);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            LivingEntity target = targetData.getTarget((ServerLevel) level);
            if (target != null && !target.getType().is(ModTags.CANT_ROOT)) {
                GildedChainsEntity chains = new GildedChainsEntity(level, entity);
                chains.setDuration(getDuration(spellLevel, entity));
                // Facing the caster, so the padlock hangs on the caster's side
                Vec3 toCaster = entity.position().subtract(target.position());
                chains.moveTo(target.getX(), target.getY(), target.getZ(), (float) (Mth.atan2(toCaster.z, toCaster.x) * Mth.RAD_TO_DEG) - 90, 0);
                level.addFreshEntity(chains);
                target.stopRiding();
                target.startRiding(chains, true);
                DamageSources.applyDamage(target, getDamage(spellLevel, entity), ModSpells.GILDED_CHAINS.get().getDamageSource(chains, entity));
                GoldFx.gild(target, GILDS);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public int getDuration(int spellLevel, @Nullable LivingEntity caster) {
        return (int) (getSpellPower(spellLevel, caster) * 20);
    }

    private float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .8f;
    }
}
