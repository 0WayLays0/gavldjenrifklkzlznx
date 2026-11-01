package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.entity.fire.Supernova;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/**
 * Supernova: a blast of flame round the caster, the wider and stronger the hotter its heat (from a red flare to a
 * violet blast, see Supernova).
 */
@AutoSpellConfig
public class SupernovaSpell extends StarfireSpell {
    public SupernovaSpell(int tier) {
        super("supernova", tier, SpellRarity.EPIC, 5, 25, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 80;
        costsByHeat();
    }

    public static float radius(int tier) {
        return 3 + 1.6f * tier;
    }

    public float blast(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * (1 + .5f * tier);
    }


    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                heatLine(caster),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(blast(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(radius(heat(caster)), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int tier = heat(entity);
        float r = radius(tier), damage = blast(spellLevel, entity);
        Supernova.at(level, entity.position(), r, tier);
        for (LivingEntity e : SpellUtil.enemiesAround(level, entity.position().add(0, 1, 0), r, entity, x -> true)) {
            FireFx.burn(e, damage, this, entity, entity, tier);
            Vec3 away = e.position().subtract(entity.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            e.setDeltaMovement(away.scale((.8 + .2 * tier) * resist).add(0, .45 * resist, 0));
            e.hurtMarked = true;
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
