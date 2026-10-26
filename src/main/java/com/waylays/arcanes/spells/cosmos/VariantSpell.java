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
 * Variant: a variant of the caster from another branch of time scans in beside them and casts one of the other spells
 * of their spellbook (any but summonings and held spells), at the level it holds there, at what they are looking at.
 * At higher levels, a second variant comes on the other side.
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
        return spellLevel >= 4 ? 2 : 1;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.arcanes_oublies.variants", variants(spellLevel)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            Vec3 forward = Vec3.directionFromRotation(0, entity.getYRot());
            // Local left (+X when facing +Z): the first variant on the right, the second on the left
            Vec3 left = new Vec3(forward.z, 0, -forward.x);
            for (int k = 0; k < variants(spellLevel); k++) {
                Vec3 side = left.scale(k == 0 ? -1.6 : 1.6).add(forward.scale(.4));
                Vec3 at = SpellUtil.groundAt(level, entity.position().add(side), 3, 2);
                CosmicVariant.scanIn(level, entity, at);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
