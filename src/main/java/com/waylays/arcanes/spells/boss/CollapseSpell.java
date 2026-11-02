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
 * Collapse (ultimate): a sphere of utter black opens over the spot aimed at and drags in everything round it for two
 * seconds; then it falls in on itself, crushing what is at its heart, and bursts, blasting everything outward.
 */
@AutoSpellConfig
public class CollapseSpell extends BossSpell {
    public static final float PULL = 12, CORE = 4, BLAST = 12;
    /** Ticks it drags things in before it falls in on itself. */
    public static final int DRAG = 40;
    public static final double RANGE = 28;

    public CollapseSpell() {
        super("collapse", SpellRarity.LEGENDARY, 3, 90, BossAnimations.SKY_CALL);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 28;
        this.spellPowerPerLevel = 6;
        this.castTime = 0;
        this.baseManaCost = 220;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(BLAST, 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.END_PORTAL_SPAWN);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 center = aimedGround(level, entity, RANGE);
            float damage = getDamage(spellLevel, entity);
            BossTelegraph core = BossTelegraph.circle(level, entity, this, spellLevel, center, CORE).style(BossTelegraph.Style.VOID).delayed(DRAG).damage(damage)
                    .part("core").spawn();
            BossTelegraph.ring(level, entity, this, spellLevel, center, CORE, BLAST).style(BossTelegraph.Style.VOID).delayed(DRAG + 8).damage(damage * .55f).part("blast")
                    .spawn();
            int fire = core.getFireTick();
            int dragFrom = fire - DRAG - now(level);
            BossProp.scene(level, BossProp.Kind.COLLAPSE, BossTelegraph.Style.VOID, PULL, now(level), fire + 30, center, center.add(0, 4.5, 0), center, fire - now(level), 0);
            // It drags everything round it in, for as long as it hangs there
            for (int k = Math.max(0, dragFrom); k < fire - now(level); k += 2) {
                BossScheduler.after(server, k, l -> drag(l, entity, center));
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        boolean core = "core".equals(t.getPart());
        for (LivingEntity target : t.targetsInside()) {
            if (t.strike(target, t.getDamage())) {
                if (core) {
                    target.setDeltaMovement(target.getDeltaMovement().add(0, .8, 0));
                    target.hurtMarked = true;
                } else {
                    knockAway(target, c, 1.4, .6);
                }
            }
        }
        BossFx.shockwave(level, c, t.getSize() * 2.1f, t.getStyle().zone);
        SpellUtil.particles(level, ParticleTypes.REVERSE_PORTAL, c.x, c.y + 2, c.z, core ? 80 : 40, t.getSize() * .5, 1.5, t.getSize() * .5, .3);
        if (core) {
            SpellUtil.particles(level, ParticleTypes.FLASH, c.x, c.y + 3, c.z, 2, 0, 0, 0, 0);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3f, .4f);
        } else {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4f, .5f);
        }
        SpellUtil.cameraShake(level, c, core ? 14 : 24, 28);
    }

    /** One pull of the drag: everything near (but its caster's side) drawn toward its heart. */
    private static void drag(ServerLevel level, LivingEntity caster, Vec3 center) {
        for (LivingEntity e : SpellUtil.enemiesAround(level, center.add(0, 1, 0), PULL, caster, x -> true)) {
            Vec3 in = center.subtract(e.position()).multiply(1, 0, 1);
            double d = in.length();
            if (d > .6) {
                double resist = Math.max(0, 1 - e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
                e.setDeltaMovement(e.getDeltaMovement().scale(.5).add(in.scale(.16 * resist / d)).add(0, .04, 0));
                e.hurtMarked = true;
            }
        }
        if (level.getGameTime() % 10 == 0) {
            level.playSound(null, center.x, center.y + 4, center.z, SoundEvents.PORTAL_AMBIENT, SoundSource.HOSTILE, 2f, .5f);
        }
    }
}
