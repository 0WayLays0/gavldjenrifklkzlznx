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
 * Brass Automaton: a walking machine of brass and copper near three blocks tall rises before the caster in a burst of
 * steam and fights for them for twenty seconds: it strides to the nearest enemy, winds back its piston arm and punches,
 * flinging what it hits; with no enemy near, it keeps by its master.
 */
@AutoSpellConfig
public class BrassAutomatonSpell extends ClockworkSpell {
    public static final int LIFE = 420;

    public BrassAutomatonSpell() {
        super("brass_automaton", SpellRarity.EPIC, 5, 50, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 100;
    }

    public float punch(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .9f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(punch(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vector3f f = facing(entity);
            Vec3 at = SpellUtil.groundAt(level, entity.position().add(f.x * 2.2, 0, f.z * 2.2), 3, 3);
            ClockworkScene.spawn(server, entity, this, ClockworkScene.Kind.AUTOMATON, at, f, 1, LIFE, punch(spellLevel, entity));
            ClockworkFx.steam(level, at.add(0, .5, 0), 30, 1.2);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.5f, .7f);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1f, .8f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
