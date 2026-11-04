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
 * Petal Storm: a whirlwind of lotus petals wraps the caster for eight seconds: enemies caught in it are cut again and
 * again, swept round and blinded.
 */
@AutoSpellConfig
public class PetalStormSpell extends LotusSpell {
    public static final float RADIUS = 4.5f;
    public static final int LIFE = 175;

    public PetalStormSpell() {
        super("petal_storm", SpellRarity.EPIC, 5, 35, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 100;
    }

    public float cut(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .25f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(cut(spellLevel, caster), 1), Utils.timeFromTicks(LotusScene.STORM_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - 15, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            LotusScene.spawn(server, entity, this, LotusScene.Kind.STORM, entity.position(), new Vector3f(0, 1, 0), RADIUS, LIFE, cut(spellLevel, entity))
                    .follow(entity);
            LotusFx.petals(level, entity.position().add(0, 1, 0), 40, 2);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 1.5f, .8f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 2f, .6f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
