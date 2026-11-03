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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Infernal Checkerboard: the ground round the spot aimed at is marked out in a board of squares, five by five; the
 * squares of one colour burst in pillars of hellfire, then the others: stepping onto a square that has just burned is
 * the way out.
 */
@AutoSpellConfig
public class InfernalCheckerboardSpell extends BossSpell {
    public static final float CELL = 3;
    public static final int GRID = 5;
    /** Ticks between the two halves of the board. */
    public static final int WAVE = 22;
    public static final double RANGE = 24;

    public InfernalCheckerboardSpell() {
        super("infernal_checkerboard", SpellRarity.EPIC, 3, 30, BossAnimations.GROUND_SLAM);
        this.manaCostPerLevel = 30;
        this.baseSpellPower = 13;
        this.spellPowerPerLevel = 4;
        this.castTime = 0;
        this.baseManaCost = 110;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.length", Utils.stringTruncation(CELL * GRID, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BLAZE_AMBIENT);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 center = aimedGround(level, entity, RANGE);
            float yaw = facing(entity);
            Vec3 fwd = direction(yaw), side = new Vec3(fwd.z, 0, -fwd.x);
            float damage = getDamage(spellLevel, entity);
            int half = GRID / 2;
            int fire = 0;
            for (int gx = -half; gx <= half; gx++) {
                for (int gz = -half; gz <= half; gz++) {
                    Vec3 cell = center.add(side.scale(gx * CELL)).add(fwd.scale(gz * CELL));
                    Vec3 start = SpellUtil.groundAt(level, cell.subtract(fwd.scale(CELL * .5)), 4, 1);
                    int wave = Math.floorMod(gx + gz, 2);
                    BossTelegraph t = BossTelegraph.line(level, entity, this, spellLevel, start, yaw, CELL, CELL)
                            .style(wave == 0 ? BossTelegraph.Style.CRIMSON : BossTelegraph.Style.VOID).delayed(wave * WAVE).damage(damage).part("cell").spawn();
                    if (wave == 0) {
                        fire = t.getFireTick();
                    }
                }
            }
            // The board itself: its squares, its frame, its rooks, its hellfire
            BossProp.scene(level, BossProp.Kind.BOARD, BossTelegraph.Style.CRIMSON, CELL * GRID, now(level), fire + WAVE + 42, center, center, center, fire - now(level), yaw);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2f, .5f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.center();
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.igniteForSeconds(4);
                target.setDeltaMovement(target.getDeltaMovement().add(0, .5, 0));
                target.hurtMarked = true;
            }
        }
        SpellUtil.particles(level, ParticleTypes.FLAME, c.x, c.y + .3, c.z, 14, CELL * .35, .4, CELL * .35, .06);
        SpellUtil.particles(level, ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + .3, c.z, 6, CELL * .3, .3, CELL * .3, .04);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.2f, .6f + level.random.nextFloat() * .3f);
    }
}
