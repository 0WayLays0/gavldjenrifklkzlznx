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
 * Sandworm: the ground trembles where the caster aims; a second later a vast worm bursts out of it, hurling up whatever
 * stands there, arcs over and dives back into the sand ahead, striking again where it plunges.
 */
@AutoSpellConfig
public class SandWormSpell extends SandSpell {
    public static final double RANGE = 24;
    public static final float GIRTH = 1.4f;

    public SandWormSpell() {
        super("sand_worm", SpellRarity.EPIC, 5, 30, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 14;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 90;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.bite_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.cast_range", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, RANGE);
            Vec3 look = entity.getLookAngle().multiply(1, 0, 1);
            look = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
            SandScene.spawn(server, entity, this, SandScene.Kind.WORM, at, look.toVector3f(), GIRTH, SandScene.WORM_BURST + SandScene.WORM_LEAP * 2,
                    getPower(spellLevel, entity));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_DIG, SoundSource.PLAYERS, 2f, .6f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
