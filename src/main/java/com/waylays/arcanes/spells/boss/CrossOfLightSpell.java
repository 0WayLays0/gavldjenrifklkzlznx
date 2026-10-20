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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Cross of Light: a great cross is marked on the ground around the target and erupts in pillars of light; a moment
 * later a second cross, turned an eighth of a turn, erupts in turn. The safe ground is between the arms of the first
 * cross, then between those of the second: step aside, then step again.
 */
@AutoSpellConfig
public class CrossOfLightSpell extends BossSpell {
    public static final double RANGE = 30;
    public static final float WIDTH = 3f;
    /** The second cross erupts this long after the first. */
    public static final int SECOND_DELAY = 24;
    /** The second cross is marked this long after the first, so the two do not read as one star. */
    public static final int SECOND_MARK = 10;

    public CrossOfLightSpell() {
        super("cross_of_light", SpellRarity.RARE, 5, 22, BossAnimations.BLADE_CALL);
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    /** Half the length of an arm, from the centre of the cross. */
    public static float reach(int spellLevel) {
        return 10 + spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.reach", Utils.stringTruncation(reach(spellLevel), 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_POWER_SELECT);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 center = aimedGround(level, entity, RANGE);
            float yaw = facing(entity);
            float damage = getDamage(spellLevel, entity);
            cross(server, entity, spellLevel, center, yaw, 0, damage, BossTelegraph.Style.HOLY);
            BossScheduler.after(server, SECOND_MARK, l -> cross(l, entity, spellLevel, center, yaw + 45, SECOND_DELAY - SECOND_MARK, damage, BossTelegraph.Style.CRIMSON));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void cross(ServerLevel level, LivingEntity caster, int spellLevel, Vec3 center, float yaw, int delay, float damage, BossTelegraph.Style style) {
        float reach = reach(spellLevel);
        for (float arm : new float[]{yaw, yaw + 90}) {
            Vec3 start = center.subtract(direction(arm).scale(reach));
            BossTelegraph.line(level, caster, this, spellLevel, SpellUtil.groundAt(level, start, 6, 1), arm, 2 * reach, WIDTH)
                    .style(style).delayed(delay).damage(damage).part("arm").spawn();
        }
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        for (LivingEntity target : t.targetsInside()) {
            t.strike(target, t.getDamage());
            target.setDeltaMovement(target.getDeltaMovement().add(0, .55, 0));
            target.hurtMarked = true;
        }
        Vec3 f = t.forward();
        for (float d = 0; d <= t.getSize(); d += 2.5f) {
            Vec3 p = SpellUtil.groundAt(level, t.position().add(f.scale(d)), 4, 1);
            BossProp.spawn(level, BossProp.Kind.PILLAR, t.getStyle(), t.getSize2() * .7f, now(level), now(level), p, p);
            SpellUtil.particles(level, ParticleTypes.END_ROD, p.x, p.y + 1, p.z, 4, .4, 1.2, .4, .05);
        }
        Vec3 c = t.center();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3f, 1.6f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.5f, 1.2f);
    }
}
