package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.GemScene;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Ruby Meteor: the caster's sigil burns where they aim; a giant ruby falls burning out of the sky onto it a second and a
 * half later, striking everything round it and throwing it back, and leaves a molten pool that burns for four seconds.
 */
@AutoSpellConfig
public class RubyMeteorSpell extends GemSpell {
    public static final double RANGE = 30;
    public static final float RADIUS = 4.5f;
    public static final int LIFE = GemScene.METEOR_FALL + 95;

    public RubyMeteorSpell() {
        super("ruby_meteor", SpellRarity.EPIC, 5, 30, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 14;
        this.spellPowerPerLevel = 4;
        this.castTime = 0;
        this.baseManaCost = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.landing_damage", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(power * .12f, 1), Utils.timeFromTicks(GemScene.POOL_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimed(level, entity, RANGE);
            GemScene.spawn(server, entity, this, GemScene.Kind.METEOR, at, new Vector3f(0, 1, 0), RADIUS, LIFE, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
