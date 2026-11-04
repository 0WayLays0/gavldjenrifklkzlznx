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

/** Saw-Gear: a ring of steel teeth spinning round brass gears flies from the caster's hand, cutting through four enemies. */
@AutoSpellConfig
public class ClockworkSawbladeSpell extends ClockworkSpell {
    public ClockworkSawbladeSpell() {
        super("clockwork_sawblade", SpellRarity.COMMON, 10, 4, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    public float cut(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .6f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(cut(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.pierce", ClockworkScene.SAW_PIERCE));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 look = entity.getLookAngle();
            ClockworkScene.spawn(server, entity, this, ClockworkScene.Kind.SAW, entity.getEyePosition().add(0, -.35, 0).add(look.scale(1)), look.toVector3f(), .8f,
                    ClockworkScene.SAW_FLIGHT + ClockworkScene.BURST + 2, cut(spellLevel, entity));
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 1f, 1.5f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1f, .8f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
