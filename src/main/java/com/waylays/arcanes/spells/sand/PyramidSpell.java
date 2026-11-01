package com.waylays.arcanes.spells.sand;

import com.waylays.arcanes.entity.sand.SandScene;
import com.waylays.arcanes.entity.sand.GlassShard;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Pyramid (ultimate): a great stepped pyramid of sandstone rises out of the sand where the caster aims, walling in the
 * enemies on its ground: blinded, slowed and turned back from its sides, they wither for three seconds; then its
 * capstone flares and it falls in on them.
 */
@AutoSpellConfig
public class PyramidSpell extends SandSpell {
    public static final double RANGE = 26;
    public static final float HALF = 6;

    public PyramidSpell() {
        super("pyramid", SpellRarity.LEGENDARY, 3, 120, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 20;
        this.spellPowerPerLevel = 5;
        this.castTime = 30;
        this.baseManaCost = 250;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.crush_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(HALF, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_POWER_SELECT);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (entity.tickCount % 4 == 0) {
            SandFx.sand(level, entity.position().add(0, .3, 0), 6, 1.5);
        }
    }

    /**
     * Where the pyramid rises: on the spot aimed at, or, if its ground would reach the caster, on along the caster's aim
     * until it stands clear of it. It rises over the enemies, never round the one who called it.
     */
    public static Vec3 clearOfCaster(Level level, LivingEntity caster, Vec3 at) {
        double margin = HALF + .8;
        if (Math.abs(at.x - caster.getX()) > margin || Math.abs(at.z - caster.getZ()) > margin) {
            return at;
        }
        Vec3 look = caster.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
        // The pyramid's ground is square to the world: along the aim, its edge is this far from its middle
        double reach = margin / Math.max(Math.abs(look.x), Math.abs(look.z));
        return SpellUtil.groundAt(level, new Vec3(caster.getX() + look.x * reach, at.y, caster.getZ() + look.z * reach), 4, 1);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = clearOfCaster(level, entity, aimedSpot(level, entity, RANGE));
            SandScene.spawn(server, entity, this, SandScene.Kind.PYRAMID, at, new Vector3f(0, 1, 0), HALF, SandScene.PYRAMID_RISE + SandScene.PYRAMID_HOLD + 32,
                    getPower(spellLevel, entity));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 2.5f, .6f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
