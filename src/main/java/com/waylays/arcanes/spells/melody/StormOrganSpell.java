package com.waylays.arcanes.spells.melody;

import com.waylays.arcanes.entity.melody.MelodyScene;
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
 * Storm Organ: a great organ of gold pipes in a case of dark wood rises where the caster points, a clef at its crown;
 * it plays, and each chord calls a pillar of sound down on an enemy near it; then it sounds its last great chord, which
 * strikes every enemy round it and flings them away.
 */
@AutoSpellConfig
public class StormOrganSpell extends MelodySpell {
    public static final float RADIUS = 12;
    public static final int LIFE = MelodyScene.ORGAN_FINALE + 30;

    public StormOrganSpell() {
        super("storm_organ", SpellRarity.EPIC, 5, 50, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 9;
        this.spellPowerPerLevel = 3;
        this.castTime = 20;
        this.baseManaCost = 130;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(power, 1), Utils.timeFromTicks(MelodyScene.ORGAN_EVERY, 1)),
                Component.translatable("ui.arcanes_oublies.finale", Utils.stringTruncation(power * 1.5f, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, 20);
            // It faces the caster
            Vector3f toCaster = new Vector3f((float) (entity.getX() - at.x), 0, (float) (entity.getZ() - at.z));
            if (toCaster.lengthSquared() < 1.0E-4f) {
                toCaster = facing(entity).negate();
            }
            MelodyScene.spawn(server, entity, this, MelodyScene.Kind.ORGAN, at, toCaster.normalize(), RADIUS, LIFE, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
