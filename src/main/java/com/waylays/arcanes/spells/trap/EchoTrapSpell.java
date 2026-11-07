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

/** Echo Trap: a trap that springs again and again, on every enemy that crosses it, each time on the same enemy its slowness and weakness a step stronger. */
@AutoSpellConfig
public class EchoTrapSpell extends TrapSpell {
    public EchoTrapSpell() {
        super("echo_trap", SpellRarity.RARE, 6, 14, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.arcanes_oublies.trap.echo_trap", TrapScene.ECHO_CHARGES));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            float power = getPower(spellLevel, entity);
            Vec3 at = aimedSpot(level, entity, 24).add(0, .02, 0);
            float mult = power / 5f;
            TrapScene.spawn(server, entity, TrapScene.Kind.ECHO, at, 1, 1200, mult, 0);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1, 1);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
