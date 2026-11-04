package com.waylays.arcanes.spells.lotus;

import com.waylays.arcanes.entity.lotus.LotusScene;
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

/**
 * Lotus Throne: a great lotus opens at the caster's feet and bears them for ten seconds, a halo of light behind them:
 * they are hardened, healed, they float down gently when they fall, and every second and a half a petal flies off it
 * at the nearest enemy.
 */
@AutoSpellConfig
public class LotusThroneSpell extends LotusSpell {
    public static final int LIFE = 215;

    public LotusThroneSpell() {
        super("lotus_throne", SpellRarity.UNCOMMON, 8, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.throne"),
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(getPower(spellLevel, caster), 1),
                        Utils.timeFromTicks(LotusScene.THRONE_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - 15, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            LotusScene.spawn(server, entity, this, LotusScene.Kind.THRONE, entity.position(), facing(entity), 1.6f,
                    LIFE, getPower(spellLevel, entity)).follow(entity);
            LotusFx.petals(level, entity.position().add(0, .5, 0), 20, .8);
            LotusFx.pollen(level, entity.position().add(0, 1, 0), 10, .5);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 1.4f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 1.5f, .8f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
