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

/** War Drum: a great war drum appears beside the caster and is beaten by phantom mallets: each beat strikes the enemies round it and rallies the caster's side. */
@AutoSpellConfig
public class WarDrumSpell extends MelodySpell {
    public static final int LIFE = MelodyScene.DRUM_FIRST + MelodyScene.DRUM_EVERY * 7 + 14;

    public WarDrumSpell() {
        super("war_drum", SpellRarity.RARE, 6, 25, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 55;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(getPower(spellLevel, caster), 1), Utils.timeFromTicks(MelodyScene.DRUM_EVERY, 1)),
                Component.translatable("ui.arcanes_oublies.rallies"),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(MelodyScene.DRUM_REACH, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vector3f f = facing(entity);
            Vec3 at = SpellUtil.groundAt(level, entity.position().add(f.z * -1.8, 0, -f.x * -1.8).add(f.x * 1.2, 0, f.z * 1.2), 2, 2);
            MelodyScene.spawn(server, entity, this, MelodyScene.Kind.DRUM, at, f, (float) MelodyScene.DRUM_REACH, LIFE, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
