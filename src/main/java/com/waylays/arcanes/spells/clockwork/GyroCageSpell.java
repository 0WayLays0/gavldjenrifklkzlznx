package com.waylays.arcanes.spells.clockwork;

import com.waylays.arcanes.entity.clockwork.ClockworkScene;
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
 * Gyroscopic Cage: three toothed rings close round the enemy the caster looks at, tumbling round it on their own axles:
 * it is held in their middle, struck by arcs every half second, and when they burst apart it takes a last blow.
 */
@AutoSpellConfig
public class GyroCageSpell extends ClockworkSpell {
    public static final int LIFE = 90;
    public static final float RADIUS = 1.5f;

    public GyroCageSpell() {
        super("gyro_cage", SpellRarity.RARE, 6, 20, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 7;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.rod_strike", Utils.stringTruncation(power * .5f, 1), Utils.timeFromTicks(ClockworkScene.GYRO_EVERY, 1)),
                Component.translatable("ui.arcanes_oublies.blast_damage", Utils.stringTruncation(power * 1.5f, 1)),
                Component.translatable("ui.arcanes_oublies.hold_time", Utils.timeFromTicks(LIFE - ClockworkScene.GYRO_CLOSE - ClockworkScene.BURST, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            LivingEntity prey = aimed(level, entity, 20);
            Vec3 at = prey != null ? prey.position() : aimedSpot(level, entity, 20);
            ClockworkScene.spawn(server, entity, this, ClockworkScene.Kind.GYRO, at, facing(entity), RADIUS, LIFE, getPower(spellLevel, entity));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.2f, 1.2f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
