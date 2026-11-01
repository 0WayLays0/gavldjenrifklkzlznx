package com.waylays.arcanes.spells.sand;

import com.waylays.arcanes.entity.sand.SandLance;
import com.waylays.arcanes.entity.sand.SandScene;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Sand Veil: rings of sand whirl round the caster for ten seconds: they harden them against blows (Resistance I), turn
 * aside enemy shots and scour whatever comes close.
 */
@AutoSpellConfig
public class SandVeilSpell extends SandSpell {
    public static final int LIFE = 200;
    public static final float RADIUS = 2.4f;

    public SandVeilSpell() {
        super("sand_veil", SpellRarity.UNCOMMON, 8, 25, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE, 1)),
                Component.translatable("ui.arcanes_oublies.veil"),
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(scour(spellLevel, caster), 1), Utils.timeFromTicks(10, 1)));
    }

    public float scour(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .3f;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            SandScene.spawn(server, entity, this, SandScene.Kind.VEIL, entity.position(), new Vector3f(0, 1, 0), RADIUS, LIFE, scour(spellLevel, entity))
                    .follow(entity);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.5f, .6f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, .6f, 1.4f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
