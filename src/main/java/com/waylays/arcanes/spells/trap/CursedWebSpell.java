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

/** Cursed Web: a great web of curses spreads over the ground round the place the caster points at; every two seconds each enemy in it is laid under a new curse, in turn. */
@AutoSpellConfig
public class CursedWebSpell extends TrapSpell {
    public CursedWebSpell() {
        super("cursed_web", SpellRarity.LEGENDARY, 3, 90, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 30;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 20;
        this.baseManaCost = 150;
    }

    public static final float RADIUS = 10;

    public int life(int spellLevel) {
        return 300 + spellLevel * 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.arcanes_oublies.trap.cursed_web", Utils.timeFromTicks(TrapScene.WEB_EVERY, 1), Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            float power = getPower(spellLevel, entity);
            Vec3 at = aimedSpot(level, entity, 24).add(0, .02, 0);
            float mult = power / 5f;
            TrapScene.spawn(server, entity, TrapScene.Kind.WEB, at, RADIUS, life(spellLevel) + TrapScene.ARM, mult, 0);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
