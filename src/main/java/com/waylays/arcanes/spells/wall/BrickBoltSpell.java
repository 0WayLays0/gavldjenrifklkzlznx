package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.entity.wall.BrickBolt;
import com.waylays.arcanes.spells.SpellUtil;
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

/**
 * Brick Volley: a sigil burns under the caster and bricks tear themselves out of the ground around them, hovering at
 * their shoulders, their mortar glowing; then they are hurled one after another at the enemy looked at.
 */
@AutoSpellConfig
public class BrickBoltSpell extends WallSpell {
    public static final float RANGE = 32;

    public BrickBoltSpell() {
        super("brick_bolt", SpellRarity.COMMON, 10, 3, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    public static int bricks(int spellLevel) {
        return 3 + spellLevel / 3;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.brick_count", bricks(spellLevel)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        LivingEntity target = aimed(level, entity, RANGE);
        Vec3 aim = target != null ? target.getBoundingBox().getCenter() : entity.getEyePosition().add(entity.getLookAngle().scale(RANGE));
        Vec3 f = Vec3.directionFromRotation(0, entity.getYRot());
        Vec3 side = new Vec3(-f.z, 0, f.x);
        int n = bricks(spellLevel);
        for (int i = 0; i < n; i++) {
            // Spread in an arc behind the caster's shoulders, alternating sides
            float u = n == 1 ? 0 : i / (n - 1f) * 2 - 1;
            Vec3 at = entity.position().add(side.scale(u * 1.8)).add(f.scale(-.4 - .4 * (1 - Math.abs(u)))).add(0, 1.6 + .5 * (1 - Math.abs(u)), 0);
            BrickBolt.summon(level, entity, this, at, 8 + i * 4, target, aim, getPower(spellLevel, entity));
        }
        WallFx.sigil(level, SpellUtil.groundAt(level, entity.position(), 2, 1), 2.2f, 14 + n * 4);
        WallFx.sound(level, entity.position(), net.minecraft.sounds.SoundEvents.STONE_BREAK, 1.2f, .7f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
