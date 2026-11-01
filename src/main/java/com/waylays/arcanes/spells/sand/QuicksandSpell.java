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
 * Quicksand: the ground where the caster aims turns to quicksand for six seconds: whatever stands in it is slowed to a
 * crawl, drawn toward its middle, kept from jumping out, and hurt every second.
 */
@AutoSpellConfig
public class QuicksandSpell extends SandSpell {
    public static final double RANGE = 20;
    public static final float RADIUS = 3.5f;
    public static final int LIFE = 120;

    public QuicksandSpell() {
        super("quicksand", SpellRarity.UNCOMMON, 8, 14, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_per_second", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, RANGE);
            SandScene.spawn(server, entity, this, SandScene.Kind.QUICKSAND, at, new Vector3f(0, 1, 0), RADIUS, LIFE, getPower(spellLevel, entity));
            SandFx.sand(level, at.add(0, .3, 0), 30, RADIUS * .4);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 2f, .5f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
