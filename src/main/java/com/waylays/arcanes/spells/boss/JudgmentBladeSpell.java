package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossProp;
import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.BlockPos;
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
 * Judgment Blade: a giant blade of light rises behind the caster, raised over a long strip of ground marked in front
 * of them, then comes down on it with all its weight. The strip keeps burning: a second later it erupts in pillars of
 * light, for whoever stayed in it after the blow.
 */
@AutoSpellConfig
public class JudgmentBladeSpell extends BossSpell {
    public static final float WIDTH = 3.2f;
    /** The pillars rise this long after the blow. */
    public static final int ERUPTION_DELAY = 20;

    public JudgmentBladeSpell() {
        super("judgment_blade", SpellRarity.RARE, 5, 22, BossAnimations.BLADE_CALL);
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 13;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 55;
    }

    public static float length(int spellLevel) {
        return 16 + 2 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.reach", Utils.stringTruncation(length(spellLevel), 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float yaw = facing(entity);
        Vec3 dir = direction(yaw);
        Vec3 start = feet(entity).add(dir.scale(1.5));
        float length = length(spellLevel);
        float damage = getDamage(spellLevel, entity);
        BossTelegraph slam = BossTelegraph.line(level, entity, this, spellLevel, start, yaw, length, WIDTH)
                .style(BossTelegraph.Style.HOLY).damage(damage).part("slam").spawn();
        BossTelegraph.line(level, entity, this, spellLevel, start, yaw, length, WIDTH)
                .style(BossTelegraph.Style.CRIMSON).delayed(ERUPTION_DELAY).damage(damage * .5f).part("eruption").spawn();
        BossProp.spawn(level, BossProp.Kind.BLADE, BossTelegraph.Style.HOLY, length, now(level), slam.getFireTick(), start, start.add(dir.scale(length)));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 dir = t.forward();
        boolean slam = t.getPart().equals("slam");
        for (LivingEntity target : t.targetsInside()) {
            t.strike(target, t.getDamage());
            if (slam) {
                // Thrown aside, out of the blade's way
                Vec3 side = new Vec3(dir.z, 0, -dir.x);
                double across = target.position().subtract(t.position()).dot(side);
                knockAway(target, target.position().subtract(side.scale(Math.signum(across == 0 ? 1 : across))), .8, .45);
            } else {
                target.setDeltaMovement(target.getDeltaMovement().add(0, .8, 0));
                target.hurtMarked = true;
            }
        }
        for (float d = 0; d <= t.getSize(); d += 2) {
            Vec3 p = SpellUtil.groundAt(level, t.position().add(dir.scale(d)), 4, 1);
            if (slam) {
                SpellUtil.particles(level, ParticleTypes.SWEEP_ATTACK, p.x, p.y + .4, p.z, 1, .5, 0, .5, 0);
                SpellUtil.particles(level, BossFx.dust(t.getStyle().rune, 2f), p.x, p.y + .3, p.z, 6, .8, .2, .8, .05);
                Utils.createTremorBlock(level, BlockPos.containing(p.x, p.y - .5, p.z), .25f);
            } else if (d % 4 == 0) {
                BossProp.spawn(level, BossProp.Kind.PILLAR, BossTelegraph.Style.HOLY, t.getSize2() * .8f, now(level), now(level), p, p);
            }
        }
        Vec3 mid = t.center();
        if (slam) {
            level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 3f, .5f);
            level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.5f, .6f);
            SpellUtil.cameraShake(level, mid, 15, t.getSize());
        } else {
            level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3f, 1.4f);
        }
    }
}
