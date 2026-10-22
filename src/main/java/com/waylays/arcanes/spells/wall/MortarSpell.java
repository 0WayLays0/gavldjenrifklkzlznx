package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.BrickBolt;
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

/** Mortar: a lump of bricks lobbed high onto the spot the caster points at; it bursts where it lands. */
@AutoSpellConfig
public class MortarSpell extends WallSpell {
    public static final float RANGE = 26;
    public static final int FLIGHT = 22;

    public MortarSpell() {
        super("mortar", SpellRarity.UNCOMMON, 8, 8, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(BrickBolt.MORTAR_RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 to = aimedSpot(level, entity, RANGE);
        Vec3 from = entity.getEyePosition().add(entity.getLookAngle().scale(.8));
        Vec3 velocity = lob(from, to, FLIGHT, .05);
        BrickBolt.shoot(level, entity, this, BrickBolt.Kind.MORTAR, from, velocity, getPower(spellLevel, entity));
        WallFx.sound(level, from, net.minecraft.sounds.SoundEvents.CROSSBOW_SHOOT, 1f, .5f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** The launch velocity to land on {@code to} after {@code ticks}, under {@code gravity}. */
    public static Vec3 lob(Vec3 from, Vec3 to, int ticks, double gravity) {
        Vec3 d = to.subtract(from);
        return new Vec3(d.x / ticks, d.y / ticks + .5 * gravity * (ticks - 1), d.z / ticks);
    }
}
