package com.waylays.arcanes.spells.aurora;

import com.waylays.arcanes.entity.aurora.AuroraScene;
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
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Aurora Mantle: curtains of the northern lights wrap round the caster for ten seconds: they are hardened and quickened,
 * shots that come at them are turned aside, and enemies who come close are seared and slowed.
 */
@AutoSpellConfig
public class AuroraMantleSpell extends AuroraSpell {
    public static final float RADIUS = 1.6f;
    public static final int LIFE = 215;

    public AuroraMantleSpell() {
        super("aurora_mantle", SpellRarity.RARE, 6, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public float sear(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .5f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.mantle"),
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(sear(spellLevel, caster), 1),
                        Utils.timeFromTicks(AuroraScene.MANTLE_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - 15, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            AuroraScene.spawn(server, entity, this, AuroraScene.Kind.MANTLE, entity.position(), facing(entity), RADIUS, LIFE, sear(spellLevel, entity)).follow(entity);
            AuroraFx.lights(level, entity.position().add(0, 1, 0), 24, .8);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 1.5f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
