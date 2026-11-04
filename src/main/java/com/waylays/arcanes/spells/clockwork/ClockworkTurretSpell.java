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

/** Clockwork Turret: a brass turret unfolds on its tripod where the caster aims and fires its spinning barrels at the nearest enemy in sight for ten seconds. */
@AutoSpellConfig
public class ClockworkTurretSpell extends ClockworkSpell {
    public static final int LIFE = 212;

    public ClockworkTurretSpell() {
        super("clockwork_turret", SpellRarity.RARE, 6, 25, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    public float shot(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .45f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.rod_strike", Utils.stringTruncation(shot(spellLevel, caster), 1),
                        Utils.timeFromTicks(ClockworkScene.TURRET_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(ClockworkScene.TURRET_RANGE, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, 8);
            ClockworkScene.spawn(server, entity, this, ClockworkScene.Kind.TURRET, at, facing(entity), 1, LIFE, shot(spellLevel, entity));
            ClockworkFx.steam(level, at.add(0, .5, 0), 10, .6);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1f, 1.4f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
