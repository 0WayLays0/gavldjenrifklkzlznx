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

/** Amethyst Spikes: clusters of amethyst burst out of the ground one after another along a line ahead, impaling and tossing up what stands there. */
@AutoSpellConfig
public class AmethystSpikesSpell extends GemSpell {
    public AmethystSpikesSpell() {
        super("amethyst_spikes", SpellRarity.COMMON, 10, 6, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    public float spike(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .8f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(spike(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.launch"),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(GemScene.SPIKES, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = SpellUtil.groundAt(level, entity.position(), 2, 1);
            float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
            GemScene.spawn(server, entity, this, GemScene.Kind.SPIKES, at, new Vector3f(-Mth.sin(yaw), 0, Mth.cos(yaw)), 1, GemScene.SPIKES + 20,
                    spike(spellLevel, entity));
            GemFx.sigil(level, at, Gem.AMETHYST, 1.6f, 14);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
