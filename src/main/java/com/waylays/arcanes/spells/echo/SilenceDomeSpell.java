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
 * Dome of Silence: a hush of the deep dark spreads round the caster in a dome; while it stands, enemies inside it cannot
 * cast spells, are slowed and ring with Resonance, the shots they loose fall short, and the caster's side is hardened.
 */
@AutoSpellConfig
public class SilenceDomeSpell extends EchoSpell {
    public static final float RADIUS = 6;

    public SilenceDomeSpell() {
        super("silence_dome", SpellRarity.EPIC, 5, 40, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 80;
    }

    public static int life(int spellLevel) {
        return 140 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 c = SpellUtil.groundAt(level, entity.position(), 3, 1);
            EchoScene.spawn(server, entity, this, EchoScene.Kind.DOME, c, new Vector3f(0, 1, 0), RADIUS, life(spellLevel), 0);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.6f, .6f);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.6f, .6f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
