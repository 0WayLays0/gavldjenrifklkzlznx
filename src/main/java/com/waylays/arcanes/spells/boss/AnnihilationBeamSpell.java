package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.AnnihilationBeam;
import com.waylays.arcanes.entity.boss.BossTelegraph;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Annihilation Beam: a rune circle stands up before the caster and charges while the strip in front of them is
 * marked, then fires a beam down the strip for two seconds (see {@link AnnihilationBeam}). It burns the marked strip
 * only: stepping out of it is enough, stepping back in is not wise.
 */
@AutoSpellConfig
public class AnnihilationBeamSpell extends BossSpell {
    public static final float WIDTH = 2.6f;

    public AnnihilationBeamSpell() {
        super("annihilation_beam", SpellRarity.EPIC, 3, 30, BossAnimations.BEAM);
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 90;
    }

    public static float length(int spellLevel) {
        return 26 + 2 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        int pulses = AnnihilationBeam.DURATION / AnnihilationBeam.PULSE;
        return List.of(
                Component.translatable("ui.arcanes_oublies.pulse_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1), pulses),
                Component.translatable("ui.arcanes_oublies.reach", Utils.stringTruncation(length(spellLevel), 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_POWER_SELECT);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float yaw = facing(entity);
        Vec3 start = feet(entity).add(direction(yaw).scale(.8));
        float length = length(spellLevel);
        BossTelegraph warning = BossTelegraph.line(level, entity, this, spellLevel, start, yaw, length, WIDTH)
                .style(BossTelegraph.Style.CRIMSON).part("beam").spawn();
        AnnihilationBeam.spawn(level, entity, this, start, yaw, length, WIDTH, warning.getFireTick(), getDamage(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** The beam itself does the burning, for two seconds after its warning runs out. */
    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
    }
}
