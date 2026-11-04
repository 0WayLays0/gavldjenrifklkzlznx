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
 * Lotus Mandala: a vast mandala of lotus petals is drawn in light on the ground where the caster aims; then its three
 * rings flare one after another, from its heart outward, striking whatever stands on each and lifting it.
 */
@AutoSpellConfig
public class LotusMandalaSpell extends LotusSpell {
    public static final double RANGE = 28;
    public static final float RADIUS = 7;
    public static final int LIFE = LotusScene.MANDALA_DRAWN + 2 * LotusScene.MANDALA_STEP + 30;

    public LotusMandalaSpell() {
        super("lotus_mandala", SpellRarity.EPIC, 5, 30, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 90;
    }

    public float flare(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .6f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.waves", 3, Utils.stringTruncation(flare(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.launch"),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, RANGE);
            LotusScene.spawn(server, entity, this, LotusScene.Kind.MANDALA, at, new Vector3f(0, 1, 0), RADIUS, LIFE, flare(spellLevel, entity));
            LotusFx.ring(level, at, RADIUS, 48);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
