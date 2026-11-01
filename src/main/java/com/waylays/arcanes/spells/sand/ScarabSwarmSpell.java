package com.waylays.arcanes.spells.sand;

import com.waylays.arcanes.entity.sand.SandLance;
import com.waylays.arcanes.entity.sand.SandScene;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Scarab Swarm: a swarm of golden scarabs pours from the caster's hand and hunts the nearest enemy, biting it again and
 * again for five seconds.
 */
@AutoSpellConfig
public class ScarabSwarmSpell extends SandSpell {
    public static final int LIFE = 100;

    public ScarabSwarmSpell() {
        super("scarab_swarm", SpellRarity.COMMON, 10, 10, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.bite_damage", Utils.stringTruncation(bite(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE, 1)));
    }

    public float bite(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .3f;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = entity.getEyePosition().add(entity.getLookAngle().scale(1.2)).subtract(0, .4, 0);
            SandScene.spawn(server, entity, this, SandScene.Kind.SWARM, at, entity.getLookAngle().toVector3f(), .7f, LIFE, bite(spellLevel, entity));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BEEHIVE_EXIT, SoundSource.PLAYERS, 1.5f, .6f);
            SandFx.sand(level, at, 10, .3);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
