package com.waylays.arcanes.spells.wood;

import com.waylays.arcanes.entity.wood.SeedPod;
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
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Seed Pods: three pods lobbed in an arc burst into thorns where they land, a thicket springing up (see SeedPod). */
@AutoSpellConfig
public class SeedPodsSpell extends WoodSpell {
    public static final int PODS = 3;

    public SeedPodsSpell() {
        super("seed_pods", SpellRarity.UNCOMMON, 8, 8, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.projectile_count", PODS),
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(SeedPod.BURST, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 from = entity.getEyePosition().add(entity.getLookAngle().scale(.8)).subtract(0, .2, 0);
        for (int k = 0; k < PODS; k++) {
            float spread = (k - (PODS - 1) * .5f) * 11;
            Vec3 dir = Vec3.directionFromRotation(Math.min(entity.getXRot(), 20) - 14, entity.getYRot() + spread);
            SeedPod.lob(level, entity, this, from, dir.scale(1.0 + .06 * k), getPower(spellLevel, entity));
        }
        WoodFx.sprout(level, from, .8f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
