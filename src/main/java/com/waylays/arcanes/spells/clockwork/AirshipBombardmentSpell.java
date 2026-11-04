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
 * Airship Bombardment: an airship of canvas and brass flies over the ground the caster aims at, dropping a string of
 * bombs that burst in fire and smoke.
 */
@AutoSpellConfig
public class AirshipBombardmentSpell extends ClockworkSpell {
    public static final int LIFE = ClockworkScene.SHIP_FIRST + ClockworkScene.SHIP_EVERY * ClockworkScene.SHIP_BOMBS + ClockworkScene.SHIP_FALL + 30;

    public AirshipBombardmentSpell() {
        super("airship_bombardment", SpellRarity.EPIC, 5, 40, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 100;
    }

    public float bomb(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .8f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.bombs", ClockworkScene.SHIP_BOMBS, Utils.stringTruncation(bomb(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(ClockworkScene.SHIP_BLAST, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, 30);
            ClockworkScene.spawn(server, entity, this, ClockworkScene.Kind.AIRSHIP, at, facing(entity), 1, LIFE, bomb(spellLevel, entity));
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1f, 1.8f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
