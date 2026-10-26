package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.TimeDoor;
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

/**
 * Time Door: two TVA doors open, one in front of the caster and one where the caster stood five seconds ago. Whatever
 * goes into the first comes out of the second, at the same speed: the caster stepping back to where they were, a
 * monster shoved through and sent off behind, an arrow fired into it.
 */
@AutoSpellConfig
public class TimeDoorSpell extends CosmosSpell {
    /** How far back the second door opens, in ticks. */
    public static final int BACK = 100;

    public TimeDoorSpell() {
        super("time_door", SpellRarity.UNCOMMON, 5, 20, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    public static int life(int spellLevel) {
        return 160 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.door_back", Utils.timeFromTicks(BACK, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(life(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            Vec3 forward = Vec3.directionFromRotation(0, entity.getYRot());
            Vec3 in = SpellUtil.groundAt(level, entity.position().add(forward.scale(2.4)), 3, 3);
            TimeTrail.Sample past = TimeTrail.ago(entity, BACK);
            Vec3 out;
            float outYRot;
            if (past != null && past.pos().distanceTo(in) > 4 && past.pos().distanceTo(entity.position()) > 3) {
                out = past.pos();
                outYRot = past.yRot();
            } else {
                // Nowhere else to go back to: the second door opens well behind
                out = SpellUtil.groundAt(level, entity.position().subtract(forward.scale(9)), 4, 3);
                outYRot = entity.getYRot() + 180;
            }
            TimeDoor.gate(level, entity, in, entity.getYRot() + 180, out, outYRot, life(spellLevel));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
