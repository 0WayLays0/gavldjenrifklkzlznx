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
 * Iron Maidens: round each of the three foes nearest the caster (or the spot aimed at, if there are none) a casket of
 * black iron rises out of the ground, its doors open, spikes inside; when the warning runs out the doors slam shut on
 * whoever is still in it, holding them fast and bleeding them.
 */
@AutoSpellConfig
public class IronMaidensSpell extends BossSpell {
    public static final float RADIUS = 2.0f;
    public static final double RANGE = 30;
    public static final int MAIDENS = 3;
    /** Ticks longer than other marks: they are blows worth dodging. */
    public static final int EXTRA = 10;

    public IronMaidensSpell() {
        super("iron_maidens", SpellRarity.EPIC, 3, 40, BossAnimations.BEAM);
        this.manaCostPerLevel = 40;
        this.baseSpellPower = 22;
        this.spellPowerPerLevel = 5;
        this.castTime = 0;
        this.baseManaCost = 160;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.IRON_DOOR_OPEN);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            List<LivingEntity> near = foes(level, entity, entity.position(), RANGE, MAIDENS);
            List<Vec3> spots = near.isEmpty() ? List.of(aimedGround(level, entity, RANGE))
                    : near.stream().map(e -> SpellUtil.groundAt(level, e.position(), 4, 1)).toList();
            float damage = getDamage(spellLevel, entity);
            for (Vec3 spot : spots) {
                BossTelegraph shut = BossTelegraph.circle(level, entity, this, spellLevel, spot, RADIUS).style(BossTelegraph.Style.CRIMSON).delayed(EXTRA).damage(damage)
                        .part("maiden").spawn();
                int fire = shut.getFireTick();
                // Its doors face the caster
                float yaw = yawOf(entity.getX() - spot.x, entity.getZ() - spot.z);
                BossProp.scene(level, BossProp.Kind.MAIDEN, BossTelegraph.Style.CRIMSON, RADIUS, now(level), fire + 32, spot, spot, spot, fire - now(level), yaw);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                // Held fast, and bleeding
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 5));
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
                target.setDeltaMovement(Vec3.ZERO);
                target.hurtMarked = true;
            }
        }
        SpellUtil.particles(level, BossFx.dust(t.getStyle().zone, 1.4f), c.x, c.y + 1.5, c.z, 40, .6, 1.2, .6, .02);
        SpellUtil.particles(level, ParticleTypes.CRIT, c.x, c.y + 1.5, c.z, 30, .5, 1, .5, .3);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.IRON_DOOR_CLOSE, SoundSource.HOSTILE, 2.5f, .5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5f, .6f);
        SpellUtil.cameraShake(level, c, 8, 10);
    }
}
