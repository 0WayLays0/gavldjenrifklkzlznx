package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.GemScene;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Amethyst Chimes: amethyst crystals hang ringing from a ring of gold over the caster's head for twelve seconds. Every
 * second they ring: a wave of sound strikes up to three enemies in sight and slows them, and enemy shots near the caster
 * shatter.
 */
@AutoSpellConfig
public class AmethystChimesSpell extends GemSpell {
    public static final int LIFE = 252;

    public AmethystChimesSpell() {
        super("amethyst_chimes", SpellRarity.EPIC, 5, 40, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 100;
    }

    public float ring(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .5f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(ring(spellLevel, caster), 1), Utils.timeFromTicks(GemScene.CHIME_EVERY, 1)),
                Component.translatable("ui.arcanes_oublies.chime_targets", GemScene.CHIME_TARGETS),
                Component.translatable("ui.arcanes_oublies.shatters_shots"),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - 12, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            GemScene.spawn(server, entity, this, GemScene.Kind.CHIMES, entity.position(), new Vector3f(0, 0, 1), 1, LIFE, ring(spellLevel, entity)).follow(entity);
            GemFx.sigil(level, SpellUtil.groundAt(level, entity.position(), 2, 1), Gem.AMETHYST, 2, 20);
            GemFx.chime(level, entity.position().add(0, 2.5, 0), 1.5f, 1.2f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
