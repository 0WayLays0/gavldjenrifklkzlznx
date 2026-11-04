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
 * Roots of the Mire: the ground where the caster aims turns to black mud, where the knotted roots of the lotus stir;
 * enemies in it sink and are dragged toward its middle, and every second and a half the roots rise and seize them.
 */
@AutoSpellConfig
public class MireRootsSpell extends LotusSpell {
    public static final double RANGE = 24;
    public static final float RADIUS = 4;
    public static final int LIFE = 110;

    public MireRootsSpell() {
        super("mire_roots", SpellRarity.RARE, 6, 20, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    public float seize(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .5f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(seize(spellLevel, caster), 1),
                        Utils.timeFromTicks(LotusScene.ROOTS_EVERY, 1)),
                Component.translatable("ui.arcanes_oublies.rooted", Utils.timeFromTicks(30, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - 10, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, RANGE);
            LotusScene.spawn(server, entity, this, LotusScene.Kind.ROOTS, at, new Vector3f(0, 1, 0), RADIUS, LIFE, seize(spellLevel, entity));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.MUD_STEP, SoundSource.PLAYERS, 2f, .5f);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 1.5f, .5f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
