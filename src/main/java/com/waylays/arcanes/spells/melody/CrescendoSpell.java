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

/** Crescendo: rings of sound roll out from the caster's hand, growing wider and louder as they go, and strike harder the further they have travelled. */
@AutoSpellConfig
public class CrescendoSpell extends MelodySpell {
    public CrescendoSpell() {
        super("crescendo", SpellRarity.UNCOMMON, 8, 8, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.arcanes_oublies.crescendo_damage", Utils.stringTruncation(power * .6f, 1), Utils.stringTruncation(power * 1.4f, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 look = entity.getLookAngle();
            MelodyScene.spawn(server, entity, this, MelodyScene.Kind.CRESCENDO, entity.getEyePosition().add(0, -.3, 0).add(look.scale(1.2)), look.toVector3f(),
                    MelodyScene.CRESC_TO, MelodyScene.CRESC_FLIGHT + MelodyScene.BURST + 2, getPower(spellLevel, entity));
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.2f, .6f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
