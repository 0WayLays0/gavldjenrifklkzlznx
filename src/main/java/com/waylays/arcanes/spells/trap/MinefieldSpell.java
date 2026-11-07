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

/** Minefield: seven mines scattered round the place the caster points at, each laying its own curse: poison, slowness, weakness, withering, blindness, bleeding, sundered armour. */
@AutoSpellConfig
public class MinefieldSpell extends TrapSpell {
    public MinefieldSpell() {
        super("minefield", SpellRarity.EPIC, 5, 25, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 70;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.arcanes_oublies.trap.minefield", TrapScene.CURSES));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            float power = getPower(spellLevel, entity);
            Vec3 at = aimedSpot(level, entity, 24).add(0, .02, 0);
            float mult = power / 5f;
            for (int k = 0; k < TrapScene.CURSES; k++) {
                float a = k * Mth.TWO_PI / TrapScene.CURSES;
                float r = k == 0 ? 0 : 2.6f;
                Vec3 p = at.add(Mth.cos(a) * r, 0, Mth.sin(a) * r);
                TrapScene.spawn(server, entity, TrapScene.Kind.MINE, com.waylays.arcanes.spells.SpellUtil.groundAt(level, p, 2, 1).add(0, .02, 0), .6f, 1200, mult, k);
            }
            level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1.2f, .7f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
