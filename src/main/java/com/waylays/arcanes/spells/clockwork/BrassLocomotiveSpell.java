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
 * Brass Locomotive: rails and sleepers lay themselves down beside the caster and ahead of them in a line of blue light,
 * then a red and brass steam engine charges down them, flinging the enemies on the track aside, and bursts into steam at
 * its end.
 */
@AutoSpellConfig
public class BrassLocomotiveSpell extends ClockworkSpell {
    public BrassLocomotiveSpell() {
        super("brass_locomotive", SpellRarity.EPIC, 5, 35, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 14;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 90;
    }

    public float ram(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * 1.3f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(ram(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(ClockworkScene.LOCO_LENGTH, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vector3f f = facing(entity);
            // The track runs just to the caster's right, from a little behind them
            Vec3 at = SpellUtil.groundAt(level, entity.position().add(-f.z * 1.2 - f.x * 1.5, 0, f.x * 1.2 - f.z * 1.5), 2, 1);
            ClockworkScene.spawn(server, entity, this, ClockworkScene.Kind.LOCOMOTIVE, at, f, 1, ClockworkScene.LOCO_RAILS + ClockworkScene.LOCO_RUN + 22,
                    ram(spellLevel, entity));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1f, 1.2f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
