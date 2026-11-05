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

/** Phantom Trumpets: five trumpets of gold rise in an arc behind the caster and sound a fanfare three times, each blast a wall of sound that strikes and hurls back the enemies before them. */
@AutoSpellConfig
public class PhantomTrumpetsSpell extends MelodySpell {
    public static final int LIFE = MelodyScene.TRUMP_FIRST + MelodyScene.TRUMP_EVERY * MelodyScene.TRUMP_BLASTS + 6;

    public PhantomTrumpetsSpell() {
        super("phantom_trumpets", SpellRarity.EPIC, 5, 35, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 3;
        this.castTime = 15;
        this.baseManaCost = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.blasts", MelodyScene.TRUMP_BLASTS, Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(MelodyScene.TRUMP_REACH, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            MelodyScene.spawn(server, entity, this, MelodyScene.Kind.TRUMPETS, entity.position(), facing(entity), (float) MelodyScene.TRUMP_REACH, LIFE,
                    getPower(spellLevel, entity)).follow(entity);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
