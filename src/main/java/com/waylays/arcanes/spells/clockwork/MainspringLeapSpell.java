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
 * Mainspring Leap: a great coiled spring rises under the caster, crushes down and lets go, flinging them high and ahead
 * and knocking back the enemies round it; where they land, a stomp of gears strikes all round. No fall damage.
 */
@AutoSpellConfig
public class MainspringLeapSpell extends ClockworkSpell {
    public MainspringLeapSpell() {
        super("mainspring_leap", SpellRarity.UNCOMMON, 8, 10, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 30;
    }

    public float stomp(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .9f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.blast_damage", Utils.stringTruncation(stomp(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(ClockworkScene.SPRING_STOMP, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = SpellUtil.groundAt(level, entity.position(), 2, 1);
            ClockworkScene.spawn(server, entity, this, ClockworkScene.Kind.SPRING, at, facing(entity), 1, 90, stomp(spellLevel, entity)).follow(entity);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 1.2f, .6f);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1f, .8f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
