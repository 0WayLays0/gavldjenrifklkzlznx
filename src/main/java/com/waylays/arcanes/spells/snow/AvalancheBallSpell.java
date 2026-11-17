package com.waylays.arcanes.spells.snow;

import com.waylays.arcanes.entity.snow.SnowScene;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.ElementSpell;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Avalanche Ball: a snowball rolls ahead, growing, bowling over and chilling those it meets, then bursts into powder. */
@AutoSpellConfig
public class AvalancheBallSpell extends ElementSpell {
    public AvalancheBallSpell() {
        super("avalanche_ball", ModSchools.SNOW_RESOURCE, SpellRarity.COMMON, 8, 6, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power * 1.4f, 1)),
                Component.translatable("ui.arcanes_oublies.snow.avalanche_ball", Math.round(SnowScene.ROLL_TIME * SnowScene.ROLL_SPEED)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = SpellUtil.groundAt(level, entity.position().add(facing(entity).scale(1.2)), 3, 1);
            SnowScene.spawn(server, entity, this, SnowScene.Kind.ROLL, at, entity.getYRot(), SnowScene.ROLL_TIME + 25, getPower(spellLevel, entity));
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SNOW_PLACE, SoundSource.PLAYERS, 1.4f, .6f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
