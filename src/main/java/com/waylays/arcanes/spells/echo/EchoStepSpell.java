package com.waylays.arcanes.spells.echo;

import com.waylays.arcanes.entity.echo.EchoScene;
import com.waylays.arcanes.entity.echo.SoundWave;
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
 * Echo Step: the caster steps through their own echo, up to ten blocks the way they look, and the echo left where they
 * stood hums louder and louder, then bursts, hurting whatever is near it.
 */
@AutoSpellConfig
public class EchoStepSpell extends EchoSpell {
    public static final double RANGE = 10;
    public static final float BURST_RADIUS = 3;

    public EchoStepSpell() {
        super("echo_step", SpellRarity.UNCOMMON, 8, 10, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)),
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(BURST_RADIUS, 1)));
    }

    /** Where the step lands: as far as the way is clear, standing on the ground. */
    public static Vec3 landing(Level level, LivingEntity caster) {
        Vec3 look = caster.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 from = caster.position().add(0, .6, 0);
        HitResult hit = level.clip(new ClipContext(from, from.add(look.scale(RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? from.add(look.scale(RANGE)) : hit.getLocation().subtract(look.scale(.6));
        return SpellUtil.groundAt(level, end, 4, 2);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 from = entity.position();
            Vec3 to = landing(level, entity);
            // The echo faces the way its caster looked
            Vec3 look = entity.getLookAngle().multiply(1, 0, 1);
            Vector3f facing = look.lengthSqr() < 1.0E-4 ? new Vector3f(0, 0, 1) : new Vector3f((float) look.x, 0, (float) look.z).normalize();
            EchoScene.spawn(server, entity, this, EchoScene.Kind.STEP, from, facing, BURST_RADIUS, EchoScene.STEP_DELAY + 14,
                    getPower(spellLevel, entity));
            EchoFx.pulse(level, from.add(0, 1, 0), 16, .4);
            entity.teleportTo(to.x, to.y, to.z);
            entity.resetFallDistance();
            EchoFx.pulse(level, to.add(0, 1, 0), 16, .4);
            level.playSound(null, from.x, from.y, from.z, SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 1.2f, .6f);
            level.playSound(null, to.x, to.y, to.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.6f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
