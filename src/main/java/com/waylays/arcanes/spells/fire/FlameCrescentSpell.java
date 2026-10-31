package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.entity.fire.FlameCrescent;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Flame Crescent: three slashes of fire fly out from the caster's hand, cutting through every enemy in their way (see FlameCrescent). */
@AutoSpellConfig
public class FlameCrescentSpell extends StarfireSpell {
    public FlameCrescentSpell() {
        super("flame_crescent", SpellRarity.RARE, 6, 8, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                heatLine(caster),
                Component.translatable("ui.arcanes_oublies.crescents", 3, Utils.stringTruncation(hot(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.pierce_line"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 look = entity.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 from = entity.getEyePosition().add(flat.scale(1.2)).subtract(0, .5, 0);
        float[] tilts = {.35f, -.35f, 0};
        float[] turns = {-.12f, .12f, 0};
        for (int k = 0; k < 3; k++) {
            Vec3 dir = new Vec3(look.x, look.y * .5, look.z).normalize().yRot(turns[k]);
            FlameCrescent.slash(level, entity, this, from.add(0, (k - 1) * .4, 0), dir, tilts[k], hot(spellLevel, entity), heat(entity));
        }
        FireFx.sound(level, from, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, .7f);
        FireFx.sound(level, from, SoundEvents.BLAZE_SHOOT, 1, 1.2f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
