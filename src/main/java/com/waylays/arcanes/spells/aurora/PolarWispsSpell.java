package com.waylays.arcanes.spells.aurora;

import com.waylays.arcanes.entity.aurora.AuroraScene;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/** Polar Wisps: wisps of aurora light rise round the caster, then dart each at its own enemy, trailing ribbons of light, and burst on it, marking it. */
@AutoSpellConfig
public class PolarWispsSpell extends AuroraSpell {
    public PolarWispsSpell() {
        super("polar_wisps", SpellRarity.COMMON, 10, 6, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    public static int wisps(int spellLevel) {
        return 3 + (spellLevel - 1) / 4;
    }

    public float wisp(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .8f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.wisps", wisps(spellLevel), Utils.stringTruncation(wisp(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(AuroraScene.WISP_RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            int n = wisps(spellLevel);
            float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
            for (int k = 0; k < n; k++) {
                float a = yaw + (k - (n - 1) * .5f) * .7f;
                Vector3f out = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
                AuroraScene.spawn(server, entity, this, AuroraScene.Kind.WISP, entity.position().add(out.x * .8, 1.4, out.z * .8), out, .4f, 70, wisp(spellLevel, entity))
                        .index(k);
            }
            AuroraFx.lights(level, entity.position().add(0, 1.4, 0), 12, .6);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.6f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
