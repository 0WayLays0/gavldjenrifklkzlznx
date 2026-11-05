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
 * Lullaby: a treble clef of gold rises over the place the caster points at, a staff of light circling under it, and a
 * soft tune falls with its notes: enemies there fall asleep, standing still with their weapons lowered, until a blow
 * wakes them or they are carried out of the song's reach.
 */
@AutoSpellConfig
public class LullabySpell extends MelodySpell {
    public static final float RADIUS = 5;

    public LullabySpell() {
        super("lullaby", SpellRarity.RARE, 6, 25, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 15;
        this.baseManaCost = 60;
    }

    public int life(int spellLevel) {
        return 80 + spellLevel * 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.lulls"),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, 24);
            MelodyScene.spawn(server, entity, this, MelodyScene.Kind.LULLABY, at, facing(entity), RADIUS, life(spellLevel), 0);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
