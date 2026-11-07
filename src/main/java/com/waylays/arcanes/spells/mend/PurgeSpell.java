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

/** Purge: every ailment is drawn out of one ally in a flash of white light, and they are healed for each one taken. */
@AutoSpellConfig
public class PurgeSpell extends MendSpell {
    public PurgeSpell() {
        super("purge", SpellRarity.UNCOMMON, 8, 16, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.arcanes_oublies.purge_heal", Utils.stringTruncation(power, 1), Utils.stringTruncation(power, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            float power = getPower(spellLevel, entity);
            LivingEntity ally = aimedAlly(level, entity, 24);
            MendScene.spawn(server, entity, ally, MendScene.Kind.PULSE, 30, 0);
            List<MobEffectInstance> bad = new ArrayList<>();
            for (MobEffectInstance e : ally.getActiveEffects()) {
                if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    bad.add(e);
                }
            }
            for (MobEffectInstance e : bad) {
                ally.removeEffect(e.getEffect());
            }
            ally.heal(power * (1 + bad.size()));
            level.playSound(null, ally.getX(), ally.getY(), ally.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1, 1.8f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
