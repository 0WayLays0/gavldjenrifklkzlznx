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
 * The Divine Prism: a great crystal hangs over the caster and splits the light. Twelve lines are marked round the
 * caster like the hours of a clock; one after another, a beam of each colour of the rainbow lances down from the
 * prism and sweeps out along its line, round and round. Stand between the lines.
 */
@AutoSpellConfig
public class DivinePrismSpell extends BossSpell {
    public static final int BEAMS = 12;
    /** Degrees between two lines. */
    public static final float STEP = 30;
    /** Each beam strikes this many ticks after the one before. */
    public static final int INTERVAL = 4;
    public static final float LENGTH = 18;
    public static final float WIDTH = 2.4f;
    public static final float HEIGHT = 9;

    public DivinePrismSpell() {
        super("divine_prism", SpellRarity.EPIC, 3, 40, BossAnimations.BEAM);
        this.manaCostPerLevel = 30;
        this.baseSpellPower = 11;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 130;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.beam_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.prism_beams", BEAMS),
                Component.translatable("ui.arcanes_oublies.length", Utils.stringTruncation(LENGTH, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_RESONATE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel) {
            Vec3 c = feet(entity);
            float yaw0 = facing(entity);
            float damage = getDamage(spellLevel, entity);
            int fire = 0;
            for (int k = 0; k < BEAMS; k++) {
                float yaw = yaw0 + k * STEP;
                Vec3 start = SpellUtil.groundAt(level, c.add(direction(yaw).scale(1.2)), 3, 1);
                BossTelegraph t = BossTelegraph.line(level, entity, this, spellLevel, start, yaw, LENGTH - 1.2f, WIDTH)
                        .style(BossTelegraph.Style.HOLY).delayed(k * INTERVAL).damage(damage).part("beam").spawn();
                if (k == 0) {
                    fire = t.getFireTick();
                }
            }
            // The model counts its angles round from +x toward +z; Minecraft's yaw is 90 degrees behind
            BossProp.scene(level, BossProp.Kind.PRISM, BossTelegraph.Style.HOLY, LENGTH, now(level), fire, c, c.add(0, HEIGHT, 0), c,
                    (yaw0 + 90) * Mth.DEG_TO_RAD, 0);
            level.playSound(null, c.x, c.y + HEIGHT, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2f, 1.4f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                knockAway(target, t.position(), .35, .25);
            }
        }
        Vec3 f = t.forward();
        for (int i = 0; i < 8; i++) {
            Vec3 p = t.position().add(f.scale(t.getSize() * (i + .5) / 8));
            SpellUtil.particles(level, ParticleTypes.END_ROD, p.x, p.y + .4, p.z, 2, .3, .2, .3, .05);
        }
        Vec3 c = t.center();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.4f, 1.6f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2f, .8f);
        // Each beam that lands shakes the ground a little
        SpellUtil.cameraShake(level, c, 6, t.getSize() + 10);
    }
}
