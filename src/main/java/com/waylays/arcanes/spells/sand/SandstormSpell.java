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
 * Sandstorm: a storm of sand wheels round the caster for ten seconds, going wherever they go: every half second it
 * scours the enemies caught in it and blinds them, and it blows the shots loosed at the caster off course.
 */
@AutoSpellConfig
public class SandstormSpell extends SandSpell {
    public static final float RADIUS = 6;
    public static final int LIFE = 200;

    public SandstormSpell() {
        super("sandstorm", SpellRarity.RARE, 6, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(getPower(spellLevel, caster), 1), Utils.timeFromTicks(10, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            // One storm at a time: casting again starts it afresh
            server.getEntitiesOfClass(SandScene.class, entity.getBoundingBox().inflate(8),
                    s -> s.getKind() == SandScene.Kind.STORM && entity.getUUID().equals(s.getOwnerUUID())).forEach(s -> s.discard());
            SandScene.spawn(server, entity, this, SandScene.Kind.STORM, entity.position(), new Vector3f(0, 1, 0), RADIUS, LIFE, getPower(spellLevel, entity))
                    .follow(entity);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 2f, .5f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
