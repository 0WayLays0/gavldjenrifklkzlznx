package com.waylays.arcanes.spells.explosion;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.ExplosiveOrb;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Blast Orb: an unstable orb that bursts into a double shock wave (white then violet) on impact.
 */
@AutoSpellConfig
public class BlastOrbSpell extends AbstractSpell {
    public static final float RADIUS = 3f;

    private final ResourceLocation spellId = ArcanesOublies.id("blast_orb");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.EXPLOSION_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(2)
            .build();

    public BlastOrbSpell() {
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
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
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.FIRECHARGE_USE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        ExplosiveOrb orb = new ExplosiveOrb(level, entity);
        Vec3 look = entity.getLookAngle();
        orb.setPos(entity.getEyePosition().add(look.scale(.6)).subtract(0, orb.getBbHeight() * .5, 0));
        orb.shoot(look, ExplosiveOrb.SPEED);
        orb.setDamage(getDamage(spellLevel, entity));
        level.addFreshEntity(orb);
        Vec3 hand = entity.getEyePosition().add(look.scale(.8)).subtract(0, .2, 0);
        SpellUtil.particles(level, ExplosionFx.sparks(), hand.x, hand.y, hand.z, 8, .05, .05, .05, .25);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .8f;
    }
}
