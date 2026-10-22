package com.waylays.arcanes.spells.verdigris;

import com.waylays.arcanes.entity.verdigris.AncientPillar;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Ancient Lightning Rod: a tall verdigrised column crowned with a lightning rod rises where the caster points and draws lightning on the enemies around it. */
@AutoSpellConfig
public class LightningRodSpell extends VerdigrisSpell {
    public static final float RANGE = 20;

    public LightningRodSpell() {
        super("ancient_lightning_rod", SpellRarity.RARE, 6, 20, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 55;
    }

    public static int duration(int spellLevel) {
        return 160 + 30 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.rod_strike", Utils.stringTruncation(getPower(spellLevel, caster), 1), Utils.timeFromTicks(AncientPillar.ROD_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(AncientPillar.ROD_RANGE, 1)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = SpellUtil.aimedGround(level, entity, RANGE);
        AncientPillar.raise(level, entity, this, AncientPillar.Kind.ROD, at, 2, duration(spellLevel), 1, getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
