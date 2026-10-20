package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
 * Seismic Rift: the caster strikes the ground and a fissure is marked from their feet, a stretch further every few
 * ticks; each stretch bursts open when its own warning runs out, throwing whoever stands on it into the air. The
 * danger travels along the line: run across it, not along it.
 */
@AutoSpellConfig
public class SeismicRiftSpell extends BossSpell {
    public static final float SEGMENT = 2.6f;
    public static final float WIDTH = 3.4f;
    /** A new stretch is marked every this many ticks. */
    public static final int SPREAD = 4;

    public SeismicRiftSpell() {
        super("seismic_rift", SpellRarity.RARE, 5, 20, BossAnimations.GROUND_SLAM);
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    public static int segments(int spellLevel) {
        return 7 + spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.reach", Utils.stringTruncation(segments(spellLevel) * SEGMENT + 1.5f, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.RAVAGER_ROAR);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            float yaw = facing(entity);
            Vec3 dir = direction(yaw);
            Vec3 origin = feet(entity).add(dir.scale(1.5));
            float damage = getDamage(spellLevel, entity);
            for (int k = 0; k < segments(spellLevel); k++) {
                Vec3 at = origin.add(dir.scale(k * SEGMENT));
                BossScheduler.after(server, k * SPREAD, l -> BossTelegraph.line(l, entity, this, spellLevel, SpellUtil.groundAt(l, at, 5, 1), yaw, SEGMENT, WIDTH)
                        .style(BossTelegraph.Style.EARTH).damage(damage).part("rift").spawn());
            }
            SpellUtil.cameraShake(level, origin, 10, 8);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        for (LivingEntity target : t.targetsInside()) {
            t.strike(target, t.getDamage());
            target.setDeltaMovement(target.getDeltaMovement().multiply(.3, 0, .3).add(0, .95, 0));
            target.hurtMarked = true;
        }
        Vec3 f = t.forward();
        Vec3 side = new Vec3(f.z, 0, -f.x);
        // The ground bursts up in blocks
        for (float a = 0; a <= t.getSize(); a += 1) {
            for (float s = -t.getSize2() * .5f + .5f; s < t.getSize2() * .5f; s += 1) {
                Vec3 p = t.position().add(f.scale(a)).add(side.scale(s));
                BlockPos below = BlockPos.containing(p.x, t.getY() - .5, p.z);
                Utils.createTremorBlock(level, below, .35f + level.random.nextFloat() * .35f);
                if (level.random.nextInt(3) == 0) {
                    SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(below)), p.x, t.getY() + .2, p.z, 6, .3, .2, .3, .15);
                }
            }
        }
        Vec3 c = t.center();
        SpellUtil.particles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + .3, c.z, 4, .8, .2, .8, .02);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2f, .6f + level.random.nextFloat() * .2f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.DRIPSTONE_BLOCK_BREAK, SoundSource.HOSTILE, 2f, .5f);
        SpellUtil.cameraShake(level, c, 6, 6);
    }
}
