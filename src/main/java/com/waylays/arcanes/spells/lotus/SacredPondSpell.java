package com.waylays.arcanes.spells.lotus;

import com.waylays.arcanes.entity.lotus.LotusScene;
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
 * Sacred Pond: clear water spreads round the caster, lotus leaves and flowers floating on it. Each second it heals the
 * caster's side standing in it and puts out their fires; enemies wading in it are slowed and weakened.
 */
@AutoSpellConfig
public class SacredPondSpell extends LotusSpell {
    public static final float RADIUS = 5;

    public SacredPondSpell() {
        super("sacred_pond", SpellRarity.UNCOMMON, 8, 25, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    public static int life(int spellLevel) {
        return 200 + 20 * (spellLevel - 1);
    }

    public float heal(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .6f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.heal_per_second", Utils.stringTruncation(heal(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.pond_slows"),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel) - 20, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = SpellUtil.groundAt(level, entity.position(), 2, 1);
            LotusScene.spawn(server, entity, this, LotusScene.Kind.POND, at, new Vector3f(0, 1, 0), RADIUS, life(spellLevel), heal(spellLevel, entity));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.5f, .8f);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 1.2f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
