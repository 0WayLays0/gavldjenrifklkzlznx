package com.waylays.arcanes.spells.wood;

import com.waylays.arcanes.entity.wood.WorldTree;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Optional;

/**
 * World Tree: after a long channel, a colossal tree tears its way up out of the ground in front of the caster, lashes
 * its enemies with its roots and heals its friends under its crown (see WorldTree).
 */
@AutoSpellConfig
public class WorldTreeSpell extends WoodSpell {
    public static final double RANGE = 22;
    public static final double NEAREST = 9;

    public WorldTreeSpell() {
        super("world_tree", SpellRarity.LEGENDARY, 3, 180, CastType.LONG, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 14;
        this.spellPowerPerLevel = 3;
        this.castTime = 40;
        this.baseManaCost = 250;
    }

    public static int life(int spellLevel) {
        return 400 + 100 * spellLevel;
    }

    public static int lashes(int spellLevel) {
        return 3 + spellLevel;
    }

    public static float mend(int spellLevel) {
        return 2 + spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.quake_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.root_lashes", lashes(spellLevel), Utils.timeFromTicks(WorldTree.LASH_EVERY, 1)),
                Component.translatable("ui.arcanes_oublies.heal_per_pulse", Utils.stringTruncation(mend(spellLevel), 1), Utils.timeFromTicks(20, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ROOTED_DIRT_BREAK);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.WARDEN_EMERGE);
    }

    /** While the caster chants, the ground round them stirs and the green of life gathers. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @org.jetbrains.annotations.Nullable MagicData playerMagicData) {
        if (entity.tickCount % 3 == 0) {
            float a = entity.tickCount * .4f;
            WoodFx.life(level, entity.position().add(Mth.cos(a) * 1.8, .3, Mth.sin(a) * 1.8), 3, .1);
            WoodFx.leaves(level, entity.position().add(0, .2, 0), 2, 1.5);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 spot = aimedSpot(level, entity, RANGE);
        Vec3 flat = spot.subtract(entity.position()).multiply(1, 0, 1);
        if (flat.length() < NEAREST) {
            float r = entity.getYRot() * Mth.DEG_TO_RAD;
            spot = SpellUtil.groundAt(level, entity.position().add(-Mth.sin(r) * NEAREST, 0, Mth.cos(r) * NEAREST), 4, 3);
        }
        WorldTree.raise(level, entity, this, spot, life(spellLevel), lashes(spellLevel), getPower(spellLevel, entity), mend(spellLevel));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
