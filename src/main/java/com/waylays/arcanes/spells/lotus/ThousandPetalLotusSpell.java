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
 * Thousand-Petal Lotus (ultimate): chant a moment; a pond of light opens where the caster aims and an immense lotus
 * rises from it, opening ring after ring. In full bloom a pillar of light falls through its heart: enemies around it
 * are struck, lifted, slowed and marked, the caster's side is healed and regenerates. Then its petals fall one by one
 * on the enemies still there, keen as blades, before it closes and fades.
 */
@AutoSpellConfig
public class ThousandPetalLotusSpell extends LotusSpell {
    public static final double RANGE = 32;
    public static final float RADIUS = 10;
    public static final int LIFE = 170;

    public ThousandPetalLotusSpell() {
        super("thousand_petal_lotus", SpellRarity.LEGENDARY, 3, 120, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 20;
        this.spellPowerPerLevel = 6;
        this.castTime = 30;
        this.baseManaCost = 250;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.bloom_damage", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.arcanes_oublies.petal_rain", Utils.stringTruncation(power * .15f, 1)),
                Component.translatable("ui.arcanes_oublies.bloom_heals", Utils.stringTruncation(power * .5f, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public java.util.Optional<net.minecraft.sounds.SoundEvent> getCastStartSound() {
        return java.util.Optional.of(SoundEvents.BEACON_AMBIENT);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (entity.tickCount % 4 == 0) {
            LotusFx.petals(level, entity.position().add(0, 1, 0), 6, 1.2);
            LotusFx.pollen(level, entity.position().add(0, 2.2, 0), 3, .4);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, RANGE);
            LotusScene.spawn(server, entity, this, LotusScene.Kind.THOUSAND, at, new Vector3f(0, 1, 0), RADIUS, LIFE, getPower(spellLevel, entity));
            SpellUtil.cameraShake(level, at, 15, 20);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
