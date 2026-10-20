package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossProp;
import com.waylays.arcanes.entity.boss.BossTelegraph;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Crushing Meteor: a great rune circle opens in the sky over the target and its shadow is drawn on the ground; a
 * burning meteor comes down through it and crushes the circle. A second later the shockwave rolls out in a ring
 * around the crater: whoever only just stepped out of the circle has to keep running.
 */
@AutoSpellConfig
public class CrushingMeteorSpell extends BossSpell {
    public static final double RANGE = 32;
    public static final float SKY_HEIGHT = 16;
    public static final float WAVE_WIDTH = 4;
    /** The shockwave rolls out this long after the impact. */
    public static final int WAVE_DELAY = 18;
    /** The meteor shows up in the sky this long before it hits. */
    public static final int FALL_TICKS = 26;

    public CrushingMeteorSpell() {
        super("crushing_meteor", SpellRarity.RARE, 5, 25, BossAnimations.SKY_CALL);
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public static float radius(int spellLevel) {
        return 4f + .4f * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(radius(spellLevel), 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.EVOKER_PREPARE_SUMMON);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at = aimedGround(level, entity, RANGE);
        float radius = radius(spellLevel);
        float damage = getDamage(spellLevel, entity);
        BossTelegraph impact = BossTelegraph.circle(level, entity, this, spellLevel, at, radius)
                .style(BossTelegraph.Style.EARTH).sky(SKY_HEIGHT).damage(damage).part("impact").reachUp(6).spawn();
        BossTelegraph.ring(level, entity, this, spellLevel, at, radius, radius + WAVE_WIDTH)
                .style(BossTelegraph.Style.CRIMSON).delayed(WAVE_DELAY).damage(damage * .45f).part("wave").spawn();
        // The meteor comes in at a slant, from behind the caster
        Vec3 back = at.subtract(entity.position()).multiply(1, 0, 1);
        back = back.lengthSqr() < 1 ? new Vec3(0, 0, -1) : back.normalize();
        int hit = impact.getFireTick();
        BossProp.spawn(level, BossProp.Kind.METEOR, BossTelegraph.Style.EARTH, 2.4f + .2f * spellLevel, hit - FALL_TICKS, hit,
                at.add(back.scale(-10)).add(0, SKY_HEIGHT + 8, 0), at.add(0, .8, 0));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        if (t.getPart().equals("impact")) {
            for (LivingEntity target : t.targetsInside()) {
                if (t.strike(target, t.getDamage())) {
                    target.igniteForSeconds(3);
                }
                knockAway(target, c, .9, .5);
            }
            BossFx.impact(level, c, t.getSize(), t.getStyle().zone, SoundEvents.DRAGON_FIREBALL_EXPLODE);
            // Debris thrown up from the crater
            for (int i = 0; i < 24; i++) {
                float a = level.random.nextFloat() * Mth.TWO_PI;
                float r = level.random.nextFloat() * t.getSize();
                Utils.createTremorBlock(level, BlockPos.containing(c.x + Mth.cos(a) * r, c.y - .5, c.z + Mth.sin(a) * r), .3f + level.random.nextFloat() * .4f);
            }
        } else {
            for (LivingEntity target : t.targetsInside()) {
                t.strike(target, t.getDamage());
                knockAway(target, c, 1.2, .35);
            }
            BossFx.shockwave(level, c, (t.getSize()) * 2.4f, t.getStyle().rune);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.HOSTILE, 2f, .8f);
        }
    }
}
