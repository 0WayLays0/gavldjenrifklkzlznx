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

/** Note Volley: three notes of gold fly from the caster's hand, each bending toward the enemy most ahead of it. */
@AutoSpellConfig
public class NoteVolleySpell extends MelodySpell {
    public NoteVolleySpell() {
        super("note_volley", SpellRarity.COMMON, 10, 3, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 12;
    }

    public int notes(int spellLevel) {
        return 3 + spellLevel / 4;
    }

    public float each(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .5f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.notes", notes(spellLevel), Utils.stringTruncation(each(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.seeking"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 look = entity.getLookAngle();
            int n = notes(spellLevel);
            for (int i = 0; i < n; i++) {
                // Fanned out a little, and a little up and down
                float spread = (i - (n - 1) * .5f) * .16f;
                Vec3 d = look.yRot(spread).add(0, (i % 2 == 0 ? .04 : -.04), 0).normalize();
                MelodyScene.spawn(server, entity, this, MelodyScene.Kind.NOTE, entity.getEyePosition().add(0, -.3, 0).add(d.scale(.9)), d.toVector3f(), .6f,
                        MelodyScene.NOTE_FLIGHT + MelodyScene.BURST + 2, each(spellLevel, entity));
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1f, 1.2f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, .8f, 1.6f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
