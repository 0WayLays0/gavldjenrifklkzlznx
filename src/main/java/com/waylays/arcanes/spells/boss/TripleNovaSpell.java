package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Triple Nova: three circles of fire drawn around the caster, the inner disc, then a ring, then a wider ring, each
 * bursting a moment after the one inside it. Stand clear of all three, or dance back into the first one once it has
 * burst.
 */
@AutoSpellConfig
public class TripleNovaSpell extends BossSpell {
    public static final float[] RADII = {4f, 8.5f, 13f};
    /** Each ring bursts this long after the one inside it. */
    public static final int STEP = 18;

    public TripleNovaSpell() {
        super("triple_nova", SpellRarity.EPIC, 3, 35, BossAnimations.GROUND_SLAM);
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 11;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADII[RADII.length - 1], 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BLAZE_AMBIENT);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 c = feet(entity);
        float damage = getDamage(spellLevel, entity);
        BossTelegraph.circle(level, entity, this, spellLevel, c, RADII[0])
                .style(BossTelegraph.Style.CRIMSON).damage(damage).part("nova").spawn();
        for (int i = 1; i < RADII.length; i++) {
            BossTelegraph.ring(level, entity, this, spellLevel, c, RADII[i - 1], RADII[i])
                    .style(i % 2 == 1 ? BossTelegraph.Style.EARTH : BossTelegraph.Style.CRIMSON).delayed(STEP * i).damage(damage).part("nova").spawn();
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        for (LivingEntity target : t.targetsInside()) {
            t.strike(target, t.getDamage());
            target.igniteForSeconds(2);
            knockAway(target, c, .8, .45);
        }
        // A wall of fire erupting all over the burst ring
        float inner = t.getShape() == BossTelegraph.Shape.RING ? t.getSize2() : 0;
        float outer = t.getSize();
        int n = (int) (outer * outer * .9f) + 6;
        for (int i = 0; i < n; i++) {
            float a = level.random.nextFloat() * Mth.TWO_PI;
            float r = Mth.sqrt(Mth.lerp(level.random.nextFloat(), inner * inner, outer * outer));
            Vec3 p = SpellUtil.groundAt(level, c.add(Mth.cos(a) * r, 0, Mth.sin(a) * r), 4, 1);
            SpellUtil.particles(level, ParticleTypes.FLAME, p.x, p.y + .2, p.z, 3, .2, .6, .2, .08);
            if (i % 3 == 0) {
                SpellUtil.particles(level, ParticleTypes.LAVA, p.x, p.y + .2, p.z, 1, .1, .1, .1, 0);
            }
        }
        BossFx.shockwave(level, c, outer * 2.1f, t.getStyle().zone);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.5f, .7f + outer * .02f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2f, .5f);
        SpellUtil.cameraShake(level, c, 10, outer + 6);
    }
}
