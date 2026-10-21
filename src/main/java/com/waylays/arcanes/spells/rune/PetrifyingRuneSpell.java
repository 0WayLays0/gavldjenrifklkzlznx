package com.waylays.arcanes.spells.rune;

import com.waylays.arcanes.entity.rune.PetrifyBind;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Petrifying Rune: a rune lights up under the target's feet and stone climbs its legs. It cannot walk or jump and the
 * stone grinds at it; when the hold ends, the stone shatters and hurts it once more.
 */
@AutoSpellConfig
public class PetrifyingRuneSpell extends RuneSpell {
    public static final int RANGE = 24;
    public static final float GRIND_DAMAGE = 1;

    public PetrifyingRuneSpell() {
        super("petrifying_rune", SpellRarity.RARE, 5, 20, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    public static int duration(int spellLevel) {
        return 50 + 10 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.hold_time", Utils.timeFromTicks(duration(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.shatter_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_RESONATE);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, RANGE, .35f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            LivingEntity target = targetData.getTarget((ServerLevel) level);
            if (target != null) {
                PetrifyBind.bind(level, entity, target, duration(spellLevel), GRIND_DAMAGE, getPower(spellLevel, entity));
                // A thread of glyphs from the caster's hand to the victim
                Vec3 from = entity.getEyePosition().add(entity.getLookAngle().scale(.8));
                Vec3 delta = target.position().add(0, target.getBbHeight() * .5, 0).subtract(from);
                int steps = (int) (delta.length() * 2);
                for (int i = 0; i <= steps; i++) {
                    Vec3 p = from.add(delta.scale(i / (double) Math.max(1, steps)));
                    SpellUtil.particles(level, RuneFx.GLOW_DUST, p.x, p.y, p.z, 1, .03, .03, .03, 0);
                }
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
