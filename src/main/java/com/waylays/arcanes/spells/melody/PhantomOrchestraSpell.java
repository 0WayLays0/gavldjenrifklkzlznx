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
 * Phantom Orchestra: the caster conducts. Eight violins of varnished wood appear in a ring round them and play, staves
 * of light wheel round the field in all its colours, a great clef of gold turns overhead; notes fall on the enemies, the
 * caster's side is healed, quickened and strengthened while the enemies are slowed; then the orchestra plays its finale,
 * a chord that strikes every enemy in the field, flings them away and shields the caster's side.
 */
@AutoSpellConfig
public class PhantomOrchestraSpell extends MelodySpell {
    public static final float RADIUS = 14;
    public static final int LIFE = MelodyScene.ORCH_FINALE + 40;

    public PhantomOrchestraSpell() {
        super("phantom_orchestra", SpellRarity.LEGENDARY, 3, 120, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 18;
        this.spellPowerPerLevel = 6;
        this.castTime = 30;
        this.baseManaCost = 250;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(power * .4f, 1), Utils.timeFromTicks(MelodyScene.ORCH_EVERY, 1)),
                Component.translatable("ui.arcanes_oublies.finale", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public java.util.Optional<net.minecraft.sounds.SoundEvent> getCastStartSound() {
        return java.util.Optional.of(SoundEvents.AMETHYST_BLOCK_RESONATE);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (entity.tickCount % 4 == 0) {
            MelodyFx.notes(level, entity.position().add(0, 2.2, 0), 3, .8);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = SpellUtil.groundAt(level, entity.position(), 2, 1);
            MelodyScene.spawn(server, entity, this, MelodyScene.Kind.ORCHESTRA, at, facing(entity), RADIUS, LIFE, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
