package com.waylays.arcanes.spells.wood;

import com.waylays.arcanes.entity.wood.BrambleHedge;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Bramble Hedge: a hedge of thorny brambles springs up across the way where the caster points (see BrambleHedge). */
@AutoSpellConfig
public class BrambleHedgeSpell extends WoodSpell {
    public static final double RANGE = 14;

    public BrambleHedgeSpell() {
        super("bramble_hedge", SpellRarity.UNCOMMON, 8, 15, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    public static float length(int spellLevel) {
        return 8 + spellLevel;
    }

    public static int duration(int spellLevel) {
        return 200 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.thorn_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.hedge_length", Utils.stringTruncation(length(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 spot = aimedSpot(level, entity, RANGE);
        // Never right on top of the caster
        Vec3 flat = spot.subtract(entity.position()).multiply(1, 0, 1);
        if (flat.length() < 4) {
            float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
            spot = SpellUtil.groundAt(level, entity.position().add(-Mth.sin(yaw) * 4, 0, Mth.cos(yaw) * 4), 3, 2);
        }
        BrambleHedge.grow(level, entity, this, BrambleHedge.Mode.LINE, spot, entity.getYRot(), length(spellLevel), duration(spellLevel), getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
