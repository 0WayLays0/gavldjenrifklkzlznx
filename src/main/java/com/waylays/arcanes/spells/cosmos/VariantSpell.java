package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.CosmicVariant;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Variant: variants of the caster from other branches of time scan in beside them (one, two from level 3, three at
 * level 5). Each casts one of the other spells of the caster's spellbook as it arrives; then, as long as they last,
 * every spell the caster casts (but summonings and held spells), they cast too, a beat later, at what the caster is
 * looking at.
 */
@AutoSpellConfig
public class VariantSpell extends CosmosSpell {
    public VariantSpell() {
        super("variant", SpellRarity.EPIC, 5, 40, CastType.INSTANT, SpellAnimations.SELF_CAST_TWO_HANDS);
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 70;
    }

    public static int variants(int spellLevel) {
        return spellLevel >= 5 ? 3 : spellLevel >= 3 ? 2 : 1;
    }

    public static int life(int spellLevel) {
        return 160 + 40 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.variants", variants(spellLevel)),
                Component.translatable("ui.arcanes_oublies.variants_mirror"),
                Component.translatable("ui.irons_spellbooks.duration", io.redspace.ironsspellbooks.api.util.Utils.timeFromTicks(life(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            Vec3 forward = Vec3.directionFromRotation(0, entity.getYRot());
            // Local left (+X when facing +Z): the first variant on the right, the second on the left
            Vec3 left = new Vec3(forward.z, 0, -forward.x);
            for (int k = 0; k < variants(spellLevel); k++) {
                // Right, left, then behind
                Vec3 side = k == 2 ? forward.scale(-1.8) : left.scale(k == 0 ? -1.6 : 1.6).add(forward.scale(.4));
                Vec3 at = SpellUtil.groundAt(level, entity.position().add(side), 3, 2);
                CosmicVariant.scanIn(level, entity, at, life(spellLevel));
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
