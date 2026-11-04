package com.waylays.arcanes.spells.clockwork;

import com.waylays.arcanes.entity.clockwork.ClockworkScene;
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
 * Great Mechanism: the caster winds the world's clockwork. A great gear turns under them, a toothed ring of brass runs
 * round the field, eight great gears rise round it with pistons pumping steam between them, and high above an astrolabe
 * of tumbling rings spins round a core of blue energy. Every half second the core hurls an arc at an enemy; the
 * caster's side is hardened and the enemies slowed; then the core overloads in a wave of blue light that strikes every
 * enemy in the field and flings it away, and shields the caster's side.
 */
@AutoSpellConfig
public class GreatMechanismSpell extends ClockworkSpell {
    public static final float RADIUS = 12;
    public static final int LIFE = ClockworkScene.MECH_OVERLOAD + 40;

    public GreatMechanismSpell() {
        super("great_mechanism", SpellRarity.LEGENDARY, 3, 120, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
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
                Component.translatable("ui.arcanes_oublies.rod_strike", Utils.stringTruncation(power * .3f, 1), Utils.timeFromTicks(ClockworkScene.MECH_EVERY, 1)),
                Component.translatable("ui.arcanes_oublies.blast_damage", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public java.util.Optional<net.minecraft.sounds.SoundEvent> getCastStartSound() {
        return java.util.Optional.of(SoundEvents.ANVIL_USE);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (entity.tickCount % 4 == 0) {
            ClockworkFx.arcs(level, entity.position().add(0, 2.2, 0), 4, .8);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = SpellUtil.groundAt(level, entity.position(), 2, 1);
            ClockworkScene.spawn(server, entity, this, ClockworkScene.Kind.MECHANISM, at, facing(entity), RADIUS, LIFE, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
