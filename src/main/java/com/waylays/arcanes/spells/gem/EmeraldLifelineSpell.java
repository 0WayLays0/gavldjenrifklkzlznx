package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.GemScene;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Lifeline: a beam of green light runs from a small emerald at the caster's breast to the most wounded of their side
 * nearby (or to the caster): it takes one ailment off them at once, then heals them every half second for three
 * seconds.
 */
@AutoSpellConfig
public class EmeraldLifelineSpell extends GemSpell {
    public static final double RANGE = 12;
    public static final int LIFE = 68;

    public EmeraldLifelineSpell() {
        super("emerald_lifeline", SpellRarity.COMMON, 10, 12, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    public float heal(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .4f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.heal_per_pulse", Utils.stringTruncation(heal(spellLevel, caster), 1),
                        Utils.timeFromTicks(GemScene.LIFELINE_EVERY, 1)),
                Component.translatable("ui.arcanes_oublies.cleanses"),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - 8, 1)));
    }

    /** The most wounded of the caster's side within reach (the caster themself if none is more hurt). */
    public static LivingEntity patient(Level level, LivingEntity caster) {
        LivingEntity best = caster;
        float worst = caster.getHealth() / caster.getMaxHealth();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(caster.position(), caster.position()).inflate(RANGE))) {
            if (e != caster && e.isAlive() && (SpellUtil.isAlly(caster, e) || e.isAlliedTo(caster)) && e.distanceTo(caster) <= RANGE) {
                float ratio = e.getHealth() / e.getMaxHealth();
                if (ratio < worst) {
                    worst = ratio;
                    best = e;
                }
            }
        }
        return best;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            GemScene.spawn(server, entity, this, GemScene.Kind.LIFELINE, entity.position(), new Vector3f(0, 0, 1), 1, LIFE, heal(spellLevel, entity)).follow(entity)
                    .heal(patient(level, entity));
            GemFx.chime(level, entity.position(), 1.2f, 1.4f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
