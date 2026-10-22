package com.waylays.arcanes.spells.token;

import com.waylays.arcanes.entity.token.TokenBolt;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.boss.BossScheduler;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Token Rain: soul tokens rain down on the spot the caster points at for a few seconds, each bursting where it lands. */
@AutoSpellConfig
public class TokenRainSpell extends TokenSpell {
    public static final float RADIUS = 4;
    public static final double RANGE = 24;
    public static final float HEIGHT = 10;
    /** Ticks between two tokens, how many fall, and the burst of each. */
    public static final int EVERY = 2;
    public static final float SPLASH = 1.3f;

    public TokenRainSpell() {
        super("token_rain", SpellRarity.RARE, 6, 15, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    public static int tokens(int spellLevel) {
        return 20 + 4 * spellLevel;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .35f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.token_count", tokens(spellLevel)),
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            Vec3 center = SpellUtil.aimedGround(level, entity, RANGE);
            float damage = damage(spellLevel, entity);
            for (int k = 0; k < 40; k++) {
                double a = k * Mth.TWO_PI / 40;
                SpellUtil.particles(level, TokenFx.DUST, center.x + Math.cos(a) * RADIUS, center.y + .2, center.z + Math.sin(a) * RADIUS, 1, 0, 0, 0, 0);
            }
            int n = tokens(spellLevel);
            for (int i = 0; i < n; i++) {
                BossScheduler.after(serverLevel, i * EVERY, l -> {
                    double a = l.random.nextDouble() * Mth.TWO_PI;
                    double r = Math.sqrt(l.random.nextDouble()) * RADIUS;
                    Vec3 to = SpellUtil.groundAt(l, center.add(Math.cos(a) * r, 0, Math.sin(a) * r), 4, 1);
                    Vec3 from = to.add((l.random.nextDouble() - .5) * 2, HEIGHT, (l.random.nextDouble() - .5) * 2);
                    TokenBolt.shoot(l, entity, this, TokenBolt.Mode.FALL, from, to.subtract(from), 1.4f, damage).withSplash(SPLASH);
                });
            }
            TokenFx.clink(level, center, 1.2f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
