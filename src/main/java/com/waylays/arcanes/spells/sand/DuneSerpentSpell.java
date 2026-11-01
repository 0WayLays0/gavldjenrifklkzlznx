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
 * Dune Serpent: a serpent of sand slides out of the ground in front of the caster and hunts the nearest enemy, bursting
 * up to bite it three times before it crumbles.
 */
@AutoSpellConfig
public class DuneSerpentSpell extends SandSpell {
    public static final int LIFE = 200;

    public DuneSerpentSpell() {
        super("dune_serpent", SpellRarity.RARE, 6, 18, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 7;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 55;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.bite_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.bites", SandScene.SERPENT_BITES));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 look = entity.getLookAngle().multiply(1, 0, 1);
            look = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
            Vec3 at = SpellUtil.groundAt(level, entity.position().add(look.scale(2)), 3, 1);
            SandScene.spawn(server, entity, this, SandScene.Kind.SERPENT, at, look.toVector3f(), .8f, LIFE, getPower(spellLevel, entity));
            SandFx.sand(level, at.add(0, .3, 0), 30, .6);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 2f, .5f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
