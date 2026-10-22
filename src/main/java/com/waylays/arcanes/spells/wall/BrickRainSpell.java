package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.BrickBolt;
import com.waylays.arcanes.spells.boss.BossScheduler;
import net.minecraft.server.level.ServerLevel;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Brick Rain: bricks rain down for a few seconds on the spot the caster points at, each bursting where it lands. */
@AutoSpellConfig
public class BrickRainSpell extends WallSpell {
    public static final float RANGE = 24;
    public static final float RADIUS = 3.5f;
    public static final int TICKS = 60;

    public BrickRainSpell() {
        super("brick_rain", SpellRarity.UNCOMMON, 8, 16, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    public static int bricks(int spellLevel) {
        return 12 + 2 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.brick_count", bricks(spellLevel)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, RANGE);
            float damage = getPower(spellLevel, entity);
            int n = bricks(spellLevel);
            for (int i = 0; i < n; i++) {
                int delay = i * TICKS / n;
                float a = WallFx.hash(entity.getId() * 31 + i) * Mth.TWO_PI;
                // The first brick falls right on the spot, the others all around
                float r = i == 0 ? 0 : Mth.sqrt(WallFx.hash(entity.getId() * 17 + i)) * RADIUS;
                Vec3 spot = at.add(Mth.cos(a) * r, 0, Mth.sin(a) * r);
                BossScheduler.after(server, delay, l -> {
                    if (entity.isAlive()) {
                        BrickBolt.shoot(l, entity, this, BrickBolt.Kind.FALLING, spot.add(0, 14, 0), new Vec3(0, -.5, 0), damage);
                    }
                });
            }
            WallFx.sound(level, at, net.minecraft.sounds.SoundEvents.STONE_BREAK, 1.5f, .5f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
