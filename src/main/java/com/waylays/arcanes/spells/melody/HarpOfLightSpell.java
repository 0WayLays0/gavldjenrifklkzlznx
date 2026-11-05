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

/** Harp of Light: a harp of gold appears over the caster's shoulder and follows them; its strings of light are plucked one by one, each note a ray at the nearest enemy. */
@AutoSpellConfig
public class HarpOfLightSpell extends MelodySpell {
    public static final int LIFE = 160;

    public HarpOfLightSpell() {
        super("harp_of_light", SpellRarity.RARE, 6, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(getPower(spellLevel, caster), 1), Utils.timeFromTicks(MelodyScene.HARP_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(MelodyScene.HARP_RANGE, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            MelodyScene.spawn(server, entity, this, MelodyScene.Kind.HARP, entity.position(), facing(entity), 1, LIFE, getPower(spellLevel, entity)).follow(entity);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
