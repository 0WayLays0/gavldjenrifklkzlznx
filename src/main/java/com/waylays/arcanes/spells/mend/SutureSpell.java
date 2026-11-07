package com.waylays.arcanes.spells.mend;

import com.waylays.arcanes.entity.mend.MendEvents;
import com.waylays.arcanes.entity.mend.MendScene;
import com.waylays.arcanes.entity.trap.TrapScene;
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
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Suture: one ally's wounds are stitched shut with threads of light; it heals more the deeper they are hurt (up to twice as much at the brink). */
@AutoSpellConfig
public class SutureSpell extends MendSpell {
    public SutureSpell() {
        super("suture", SpellRarity.COMMON, 10, 6, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.arcanes_oublies.suture_heal", Utils.stringTruncation(power, 1), Utils.stringTruncation(power * 2, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            float power = getPower(spellLevel, entity);
            LivingEntity ally = aimedAlly(level, entity, 24);
            MendScene.spawn(server, entity, ally, MendScene.Kind.PULSE, 30, 0);
            float missing = 1 - ally.getHealth() / ally.getMaxHealth();
            ally.heal(power * (1 + missing));
            level.playSound(null, ally.getX(), ally.getY(), ally.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1, 1.3f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
