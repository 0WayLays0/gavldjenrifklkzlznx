package com.waylays.arcanes.spells.wood;

import com.waylays.arcanes.entity.wood.LeafStorm;
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

/** Leaf Storm: a whirlwind of razor leaves gathers in front of the caster and wanders the way they look (see LeafStorm). */
@AutoSpellConfig
public class LeafStormSpell extends WoodSpell {
    public LeafStormSpell() {
        super("leaf_storm", SpellRarity.RARE, 6, 25, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public static int life(int spellLevel) {
        return 100 + 15 * spellLevel;
    }

    public float cut(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .4f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.cut_damage", Utils.stringTruncation(cut(spellLevel, caster), 1), Utils.timeFromTicks(8, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(LeafStorm.RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float r = entity.getYRot() * Mth.DEG_TO_RAD;
        Vec3 at = SpellUtil.groundAt(level, entity.position().add(-Mth.sin(r) * 4.5, 0, Mth.cos(r) * 4.5), 3, 2);
        LeafStorm.raise(level, entity, this, at, entity.getYRot(), life(spellLevel), cut(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
