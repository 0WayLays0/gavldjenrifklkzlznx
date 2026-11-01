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
 * The Chains of the Damned: four gates of red runes open round each of the foes nearest the caster and black chains
 * burst out of them, binding the spot: whoever stands in it is slowed to a crawl while the chains hold. When the
 * sentence falls the chains shatter and a column of hellfire roars up where they bound.
 */
@AutoSpellConfig
public class ChainsOfTheDamnedSpell extends BossSpell {
    public static final double RANGE = 24;
    public static final float RADIUS = 2.6f;
    /** Each spot is bound this many ticks after the one before. */
    public static final int CASCADE = 4;

    public ChainsOfTheDamnedSpell() {
        super("chains_of_the_damned", SpellRarity.EPIC, 3, 32, BossAnimations.PRISON);
        this.manaCostPerLevel = 30;
        this.baseSpellPower = 15;
        this.spellPowerPerLevel = 4;
        this.castTime = 0;
        this.baseManaCost = 110;
    }

    public static int bound(int spellLevel) {
        return 2 + spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.chained", bound(spellLevel)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.CHAIN_PLACE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            float damage = getDamage(spellLevel, entity);
            List<Vec3> spots = new ArrayList<>();
            for (LivingEntity foe : foes(level, entity, entity.position(), RANGE, bound(spellLevel))) {
                spots.add(SpellUtil.groundAt(level, foe.position(), 3, 1));
            }
            if (spots.isEmpty()) {
                spots.add(SpellUtil.aimedSpot(level, entity, RANGE));
            }
            for (int i = 0; i < spots.size(); i++) {
                Vec3 c = spots.get(i);
                BossTelegraph t = BossTelegraph.circle(level, entity, this, spellLevel, c, RADIUS)
                        .style(BossTelegraph.Style.CRIMSON).delayed(i * CASCADE).damage(damage).part("chains").spawn();
                BossProp.scene(level, BossProp.Kind.CHAINS, BossTelegraph.Style.CRIMSON, RADIUS, now(level), t.getFireTick(), c, c, c, 0, 0);
                level.playSound(null, c.x, c.y, c.z, SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 2f, .5f);
                // While the chains hold, whoever stands in their circle can barely move
                for (int tick = 12; tick < t.getFireTick() - now(level); tick += 10) {
                    BossScheduler.after(server, tick, l -> {
                        if (!t.isRemoved()) {
                            for (LivingEntity bound : t.targetsInside()) {
                                bound.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 14, 2, false, false, true));
                            }
                        }
                    });
                }
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.igniteForSeconds(5);
                target.setDeltaMovement(target.getDeltaMovement().add(0, .6, 0));
                target.hurtMarked = true;
            }
        }
        SpellUtil.particles(level, ParticleTypes.FLAME, c.x, c.y + 2, c.z, 60, .6, 2.2, .6, .08);
        SpellUtil.particles(level, ParticleTypes.LAVA, c.x, c.y + .5, c.z, 14, .8, .3, .8, 0);
        SpellUtil.particles(level, ParticleTypes.LARGE_SMOKE, c.x, c.y + 4, c.z, 20, .6, 1.5, .6, .05);
        BossFx.shockwave(level, c, RADIUS * 2.4f, t.getStyle().rune);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 3f, .6f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.5f, .5f);
        SpellUtil.cameraShake(level, c, 10, 12);
    }
}
