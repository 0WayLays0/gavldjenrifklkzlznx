package com.waylays.arcanes.spells.explosion;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.ImplosionCore;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Implosion: a point of black light opens where the caster looks, drags nearby enemies into it, contracts and
 * detonates in a blinding white sphere (see {@link ImplosionCore}).
 */
@AutoSpellConfig
public class ImplosionSpell extends AbstractSpell {
    public static final float RADIUS = 4.5f;
    public static final float RANGE = 24f;

    private final ResourceLocation spellId = ArcanesOublies.id("implosion");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(ModSchools.EXPLOSION_RESOURCE)
            .setMaxLevel(6)
            .setCooldownSeconds(30)
            .build();

    public ImplosionSpell() {
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
        this.castTime = 20;
        this.baseManaCost = 90;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.arcanes_oublies.pull_radius", Utils.stringTruncation(getPullRadius(spellLevel), 1))
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
        return Optional.of(SoundEvents.PORTAL_TRIGGER);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.BEACON_POWER_SELECT);
    }

    /** While charging: violet light gathers between the caster's hands. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        Vec3 hands = entity.getEyePosition().add(entity.getLookAngle().scale(.9)).subtract(0, .3, 0);
        SpellUtil.particles(level, ExplosionFx.ARCANE_DUST, hands.x, hands.y, hands.z, 3, .4, .4, .4, 0);
        SpellUtil.particles(level, ExplosionFx.arcaneSparks(), hands.x, hands.y, hands.z, 1, .1, .1, .1, .05);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        HitResult hit = Utils.raycastForEntity(level, entity, RANGE, true, .35f);
        Vec3 base = hit instanceof EntityHitResult entityHit
                ? entityHit.getEntity().position()
                : hit.getLocation().subtract(entity.getLookAngle().scale(.6));
        Vec3 ground = SpellUtil.groundAt(level, base, 4, 2);
        ImplosionCore core = new ImplosionCore(level, entity, getDamage(spellLevel, entity), getPullRadius(spellLevel));
        core.moveTo(ground.x, ground.y + 1.4 - core.getBbHeight() * .5, ground.z);
        level.addFreshEntity(core);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 6 blocks at level 1, +0.5 per level. */
    public float getPullRadius(int spellLevel) {
        return 5.5f + spellLevel * .5f;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 1.4f;
    }
}
