package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossProp;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Starfall: small circles of starlight are marked one after another around the target, spreading over the area; a
 * star falls into each one when its own warning runs out, so the stars come down in a cascade, in the order they were
 * marked.
 */
@AutoSpellConfig
public class StarfallSpell extends BossSpell {
    public static final double RANGE = 30;
    public static final float STAR_RADIUS = 2.4f;
    public static final float AREA = 6.5f;
    /** A new circle is marked every this many ticks. */
    public static final int CASCADE = 5;
    public static final int FALL_TICKS = 14;

    public StarfallSpell() {
        super("starfall", SpellRarity.RARE, 5, 25, BossAnimations.SKY_CALL);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public static int stars(int spellLevel) {
        return 5 + 2 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.star_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.stars", stars(spellLevel)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 center = aimedGround(level, entity, RANGE);
            float damage = getDamage(spellLevel, entity);
            List<Vec3> spots = spots(server, center, stars(spellLevel));
            for (int i = 0; i < spots.size(); i++) {
                Vec3 spot = spots.get(i);
                // Marked one after another: the warnings spread over the area like the stars after them
                BossScheduler.after(server, i * CASCADE, l -> {
                    Vec3 ground = SpellUtil.groundAt(l, spot, 6, 1);
                    BossTelegraph t = BossTelegraph.circle(l, entity, this, spellLevel, ground, STAR_RADIUS)
                            .style(BossTelegraph.Style.STAR).damage(damage).part("star").spawn();
                    BossProp.spawn(l, BossProp.Kind.STAR, BossTelegraph.Style.STAR, 1.2f, t.getFireTick() - FALL_TICKS, t.getFireTick(),
                            ground.add(l.random.nextFloat() * 6 - 3, 26, l.random.nextFloat() * 6 - 3), ground.add(0, .5, 0));
                });
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** Spots spread over the area, not on top of each other, the first ones nearest the middle. */
    private static List<Vec3> spots(ServerLevel level, Vec3 center, int count) {
        List<Vec3> out = new ArrayList<>();
        out.add(center);
        int tries = 0;
        while (out.size() < count && tries++ < 200) {
            float spread = AREA * Math.min(1, .45f + out.size() / (float) count);
            float a = level.random.nextFloat() * Mth.TWO_PI;
            float r = Mth.sqrt(level.random.nextFloat()) * spread;
            Vec3 p = center.add(Mth.cos(a) * r, 0, Mth.sin(a) * r);
            boolean free = true;
            for (Vec3 q : out) {
                free &= q.distanceToSqr(p) > STAR_RADIUS * STAR_RADIUS * 1.2;
            }
            if (free) {
                out.add(p);
            }
        }
        return out;
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        for (LivingEntity target : t.targetsInside()) {
            t.strike(target, t.getDamage());
            knockAway(target, c, .5, .3);
        }
        SpellUtil.particles(level, ParticleTypes.END_ROD, c.x, c.y + .5, c.z, 30, .3, .3, .3, .25);
        SpellUtil.particles(level, ParticleTypes.FIREWORK, c.x, c.y + .5, c.z, 20, .5, .3, .5, .15);
        BossFx.shockwave(level, c, t.getSize() * 2.2f, t.getStyle().rune);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.HOSTILE, 2f, .8f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2f, .6f);
    }
}
