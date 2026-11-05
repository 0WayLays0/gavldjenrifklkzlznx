package com.waylays.arcanes.spells.melody;

import com.waylays.arcanes.entity.melody.MelodyScene;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/** Soothing Melody: a gentle tune plays round the caster for five seconds, healing them and their allies near and drawing out poison. */
@AutoSpellConfig
public class SoothingMelodySpell extends MelodySpell {
    public static final int LIFE = 100;

    public SoothingMelodySpell() {
        super("soothing_melody", SpellRarity.UNCOMMON, 8, 20, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    public float heal(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .6f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.heal_per_pulse", Utils.stringTruncation(heal(spellLevel, caster), 1), Utils.timeFromTicks(MelodyScene.SOOTHE_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(MelodyScene.SOOTHE_RADIUS, 1)),
                Component.translatable("ui.arcanes_oublies.draws_poison"),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            MelodyScene.spawn(server, entity, this, MelodyScene.Kind.SOOTHE, entity.position(), facing(entity), (float) MelodyScene.SOOTHE_RADIUS, LIFE,
                    heal(spellLevel, entity)).follow(entity);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
