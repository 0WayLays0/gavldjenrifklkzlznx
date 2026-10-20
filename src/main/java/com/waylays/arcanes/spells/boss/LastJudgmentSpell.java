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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Last Judgment (ultimate): a giant circle of runes opens in the sky over the caster and the whole arena beneath it
 * is marked for destruction. Three small circles of green light are the only shelter. While the sentence counts down,
 * meteors crash into the arena one after another; then the judgment falls on everything that is neither the caster's
 * nor standing in a shelter.
 */
@AutoSpellConfig
public class LastJudgmentSpell extends BossSpell {
    public static final float RADIUS = 15f;
    public static final float SAFE_RADIUS = 2.4f;
    public static final int SAFE_SPOTS = 3;
    public static final int METEORS = 5;
    public static final float METEOR_RADIUS = 3.4f;
    /** Each meteor lands this long after the one before it. */
    public static final int METEOR_STEP = 8;
    /** The judgment falls this long after the first meteors' warnings run out. */
    public static final int JUDGMENT_DELAY = 50;
    public static final float SKY_HEIGHT = 20;

    public LastJudgmentSpell() {
        super("last_judgment", SpellRarity.LEGENDARY, 3, 150, BossAnimations.SKY_CALL);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 14;
        this.spellPowerPerLevel = 4;
        this.castTime = 20;
        this.baseManaCost = 300;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.arcanes_oublies.shelters", SAFE_SPOTS),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.WITHER_AMBIENT);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.WITHER_SPAWN);
    }

    /** While it is cast, the sky over the caster darkens with embers rising toward it. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .4f;
        for (int i = 0; i < 6; i++) {
            float b = a + i * Mth.TWO_PI / 6;
            SpellUtil.particles(level, BossFx.dust(0xFF2438, 1.5f), entity.getX() + Mth.cos(b) * 2, entity.getY() + (entity.tickCount % 20) * .25, entity.getZ() + Mth.sin(b) * 2, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 c = feet(entity);
        float damage = getDamage(spellLevel, entity);
        // The shelters, spread around the arena
        List<Vec3> shelters = new ArrayList<>();
        float turn = level.random.nextFloat() * Mth.TWO_PI;
        ListTag spots = new ListTag();
        for (int k = 0; k < SAFE_SPOTS; k++) {
            float a = turn + k * Mth.TWO_PI / SAFE_SPOTS;
            float r = RADIUS * (.4f + level.random.nextFloat() * .35f);
            Vec3 spot = SpellUtil.groundAt(level, c.add(Mth.cos(a) * r, 0, Mth.sin(a) * r), 6, 1);
            shelters.add(spot);
            BossTelegraph.circle(level, entity, this, spellLevel, spot, SAFE_RADIUS)
                    .style(BossTelegraph.Style.SAFE).delayed(JUDGMENT_DELAY).part("shelter").spawn();
            CompoundTag tag = new CompoundTag();
            tag.putDouble("X", spot.x);
            tag.putDouble("Z", spot.z);
            spots.add(tag);
        }
        CompoundTag payload = new CompoundTag();
        payload.put("Shelters", spots);
        BossTelegraph.circle(level, entity, this, spellLevel, c, RADIUS)
                .style(BossTelegraph.Style.CRIMSON).sky(SKY_HEIGHT).delayed(JUDGMENT_DELAY).damage(damage).part("judgment").reachUp(8).payload(payload).spawn();
        // Meteors while the sentence counts down, away from the shelters
        for (int k = 0; k < METEORS; k++) {
            Vec3 spot = meteorSpot(level, c, shelters);
            BossTelegraph meteor = BossTelegraph.circle(level, entity, this, spellLevel, spot, METEOR_RADIUS)
                    .style(BossTelegraph.Style.EARTH).delayed(k * METEOR_STEP).damage(damage * .35f).part("meteor").reachUp(6).spawn();
            int hit = meteor.getFireTick();
            BossProp.spawn(level, BossProp.Kind.METEOR, BossTelegraph.Style.EARTH, 2.2f, hit - 22, hit,
                    spot.add(level.random.nextFloat() * 10 - 5, SKY_HEIGHT + 6, level.random.nextFloat() * 10 - 5), spot.add(0, .8, 0));
        }
        SpellUtil.cameraShake(level, c, 40, RADIUS + 10);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static Vec3 meteorSpot(Level level, Vec3 c, List<Vec3> shelters) {
        Vec3 best = c;
        for (int tries = 0; tries < 20; tries++) {
            float a = level.random.nextFloat() * Mth.TWO_PI;
            float r = Mth.sqrt(level.random.nextFloat()) * (RADIUS - METEOR_RADIUS);
            Vec3 p = c.add(Mth.cos(a) * r, 0, Mth.sin(a) * r);
            boolean clear = true;
            for (Vec3 s : shelters) {
                clear &= s.distanceToSqr(p.x, s.y, p.z) > (SAFE_RADIUS + METEOR_RADIUS + 1) * (SAFE_RADIUS + METEOR_RADIUS + 1);
            }
            best = p;
            if (clear) {
                break;
            }
        }
        return SpellUtil.groundAt(level, best, 6, 1);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        switch (t.getPart()) {
            case "meteor" -> {
                for (LivingEntity target : t.targetsInside()) {
                    t.strike(target, t.getDamage());
                    target.igniteForSeconds(2);
                    knockAway(target, c, .7, .4);
                }
                BossFx.impact(level, c, t.getSize(), t.getStyle().zone, SoundEvents.DRAGON_FIREBALL_EXPLODE);
            }
            case "shelter" -> SpellUtil.particles(level, ParticleTypes.END_ROD, c.x, c.y + .5, c.z, 25, .6, 1.5, .6, .05);
            case "judgment" -> judgment(t, level, c);
            default -> {
            }
        }
    }

    private void judgment(BossTelegraph t, ServerLevel level, Vec3 c) {
        ListTag shelters = t.getPayload().getList("Shelters", Tag.TAG_COMPOUND);
        for (LivingEntity target : t.targetsInside()) {
            boolean sheltered = false;
            for (int i = 0; i < shelters.size(); i++) {
                CompoundTag s = shelters.getCompound(i);
                double dx = target.getX() - s.getDouble("X");
                double dz = target.getZ() - s.getDouble("Z");
                sheltered |= dx * dx + dz * dz <= (SAFE_RADIUS + .3) * (SAFE_RADIUS + .3);
            }
            if (!sheltered && t.strike(target, t.getDamage())) {
                target.setDeltaMovement(target.getDeltaMovement().add(0, .9, 0));
                target.hurtMarked = true;
            }
        }
        // Pillars of fire all over the arena
        for (int i = 0; i < 16; i++) {
            float a = level.random.nextFloat() * Mth.TWO_PI;
            float r = Mth.sqrt(level.random.nextFloat()) * t.getSize();
            Vec3 p = SpellUtil.groundAt(level, c.add(Mth.cos(a) * r, 0, Mth.sin(a) * r), 6, 1);
            BossProp.spawn(level, BossProp.Kind.PILLAR, BossTelegraph.Style.CRIMSON, 1.6f, now(level), now(level), p, p);
        }
        BossFx.impact(level, c, t.getSize() * .6f, t.getStyle().zone, SoundEvents.LIGHTNING_BOLT_THUNDER);
        BossFx.shockwave(level, c, t.getSize() * 2.4f, t.getStyle().rune);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 2f, .6f);
        SpellUtil.cameraShake(level, c, 40, t.getSize() * 2.5f);
    }
}
