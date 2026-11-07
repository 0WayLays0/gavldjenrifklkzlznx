package com.waylays.arcanes.spells.trap;

import com.waylays.arcanes.entity.mend.MendEvents;
import com.waylays.arcanes.entity.mend.MendScene;
import com.waylays.arcanes.entity.trap.TrapScene;
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
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Leech Trap: a leech of blood waits on the ground; it withers the first enemy to step in and, while they wither, feeds the caster's health. */
@AutoSpellConfig
public class LeechTrapSpell extends TrapSpell {
    public LeechTrapSpell() {
        super("leech_trap", SpellRarity.EPIC, 5, 20, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.arcanes_oublies.trap.leech_trap", Utils.timeFromTicks(TrapScene.LEECH_TIME, 1), Utils.stringTruncation(1 + power / 5f * .5f, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            float power = getPower(spellLevel, entity);
            Vec3 at = aimedSpot(level, entity, 24).add(0, .02, 0);
            float mult = power / 5f;
            TrapScene.spawn(server, entity, TrapScene.Kind.LEECH, at, 1, 1200, mult, 0);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1, 1);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
