package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossProp;
import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Thousand Blades: a crown of swords of light forms over the caster, pointing at the sky. Small circles are marked
 * one after another on the foes around the spot the caster aims at (then all over it); each sword rises, turns over and
 * dives into its circle as the circle's warning runs out, and stands there a moment before it breaks into light.
 */
@AutoSpellConfig
public class ThousandBladesSpell extends BossSpell {
    public static final double RANGE = 28;
    public static final float SWORD_RADIUS = 2.2f;
    public static final float AREA = 8;
    public static final float SWORD_LENGTH = 3.8f;
    /** Each sword lands this many ticks after the one before. */
    public static final int CASCADE = 3;

    public ThousandBladesSpell() {
        super("thousand_blades", SpellRarity.EPIC, 3, 40, BossAnimations.SKY_CALL);
        this.manaCostPerLevel = 30;
        this.baseSpellPower = 9;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 120;
    }

    public static int swords(int spellLevel) {
        return 10 + 4 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.blade_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.swords", swords(spellLevel)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 center = SpellUtil.aimedSpot(level, entity, RANGE);
            float damage = getDamage(spellLevel, entity);
            int count = swords(spellLevel);
            List<Vec3> spots = new ArrayList<>();
            // Every foe near the mark gets a sword (the nearest a second one), then the rest of the area is covered
            List<LivingEntity> foes = foes(level, entity, center, AREA + 2, count / 2);
            for (LivingEntity foe : foes) {
                spots.add(foe.position());
            }
            for (int i = 0; i < foes.size() && spots.size() < count / 2; i++) {
                spots.add(foes.get(i).position().add(level.random.nextFloat() * 3 - 1.5, 0, level.random.nextFloat() * 3 - 1.5));
            }
            spots.add(center);
            int tries = 0;
            while (spots.size() < count && tries++ < 300) {
                float a = level.random.nextFloat() * Mth.TWO_PI;
                float r = Mth.sqrt(level.random.nextFloat()) * AREA;
                Vec3 p = center.add(Mth.cos(a) * r, 0, Mth.sin(a) * r);
                boolean free = true;
                for (Vec3 q : spots) {
                    free &= q.distanceToSqr(p.x, q.y, p.z) > SWORD_RADIUS * SWORD_RADIUS * 1.4;
                }
                if (free) {
                    spots.add(p);
                }
            }
            Vec3 crown = entity.position().add(0, 5.2, 0);
            int now = now(level);
            for (int i = 0; i < spots.size(); i++) {
                Vec3 ground = SpellUtil.groundAt(level, spots.get(i), 6, 1);
                BossTelegraph t = BossTelegraph.circle(level, entity, this, spellLevel, ground, SWORD_RADIUS)
                        .style(BossTelegraph.Style.HOLY).delayed(i * CASCADE).damage(damage).part("sword").spawn();
                // Its place in the crown over the caster
                float a = i * Mth.TWO_PI / spots.size() + facing(entity) * Mth.DEG_TO_RAD;
                Vec3 hang = crown.add(Mth.cos(a) * 3.4, .6 * Mth.sin(a * 3), Mth.sin(a) * 3.4);
                BossProp.scene(level, BossProp.Kind.SWORD, BossTelegraph.Style.HOLY, SWORD_LENGTH, now + (i % 6), t.getFireTick(), ground, hang, ground, 0, 0);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        for (LivingEntity target : t.targetsInside()) {
            t.strike(target, t.getDamage());
            knockAway(target, c, .45, .25);
        }
        SpellUtil.particles(level, ParticleTypes.END_ROD, c.x, c.y + .6, c.z, 18, .3, .5, .3, .2);
        SpellUtil.particles(level, ParticleTypes.ENCHANTED_HIT, c.x, c.y + .5, c.z, 14, .5, .3, .5, .3);
        BossFx.shockwave(level, c, t.getSize() * 2, t.getStyle().rune);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.TRIDENT_HIT_GROUND, SoundSource.HOSTILE, 2.2f, .6f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.6f, 1.3f);
    }
}
