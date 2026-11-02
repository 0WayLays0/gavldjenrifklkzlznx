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
 * Bell of Doom: a great bronze bell rings out over the caster three times; each toll sends a ring of sound out over the
 * ground, wider each time (marked on it beforehand): whoever stands in the ring as it tolls is struck, thrown back and
 * slowed.
 */
@AutoSpellConfig
public class BellOfDoomSpell extends BossSpell {
    public static final float[] RADII = {1.5f, 5.5f, 9.5f, 13.5f};
    /** Ticks between two tolls. */
    public static final int TOLL = 16;

    public BellOfDoomSpell() {
        super("bell_of_doom", SpellRarity.EPIC, 3, 35, BossAnimations.SKY_CALL);
        this.manaCostPerLevel = 30;
        this.baseSpellPower = 14;
        this.spellPowerPerLevel = 4;
        this.castTime = 0;
        this.baseManaCost = 120;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADII[3], 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BELL_RESONATE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 c = feet(entity);
            float damage = getDamage(spellLevel, entity);
            int fire = 0;
            for (int i = 0; i < 3; i++) {
                BossTelegraph t = BossTelegraph.ring(level, entity, this, spellLevel, c, RADII[i], RADII[i + 1]).style(BossTelegraph.Style.HOLY).delayed(TOLL * i)
                        .damage(damage).part("toll").spawn();
                if (i == 0) {
                    fire = t.getFireTick();
                }
            }
            BossProp.scene(level, BossProp.Kind.BELL, BossTelegraph.Style.HOLY, RADII[3], now(level), fire + TOLL * 2 + 24, c, c.add(0, 7.5, 0), c, fire - now(level), 0);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
                knockAway(target, c, .9, .4);
            }
        }
        BossFx.shockwave(level, c, t.getSize() * 2.1f, t.getStyle().zone);
        SpellUtil.particles(level, BossFx.dust(t.getStyle().zone, 1.6f), c.x, c.y + .4, c.z, 40, t.getSize() * .6, .2, t.getSize() * .6, .02);
        level.playSound(null, c.x, c.y + 7, c.z, SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 4f, .5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2f, .5f);
        SpellUtil.cameraShake(level, c, 8, t.getSize() + 6);
    }
}
