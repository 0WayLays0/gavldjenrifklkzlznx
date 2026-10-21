package com.waylays.arcanes.spells.rune;

import com.waylays.arcanes.entity.rune.RuneBulwark;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Runic Bulwark: a wall of carved stone slabs bursts out of the ground in an arc before the caster. Nothing walks
 * through it and arrows and spells stop against it; then it crumbles. A slab is left out where someone stands.
 */
@AutoSpellConfig
public class RunicBulwarkSpell extends RuneSpell {
    public static final double DISTANCE = 3;
    /** Degrees between two slabs along the arc. */
    public static final float STEP = 22;

    public RunicBulwarkSpell() {
        super("runic_bulwark", SpellRarity.RARE, 5, 25, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    public static int slabs(int spellLevel) {
        return spellLevel >= 3 ? 7 : 5;
    }

    public static int duration(int spellLevel) {
        return 120 + 20 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.wall_slabs", slabs(spellLevel)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.DEEPSLATE_TILES_PLACE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int n = slabs(spellLevel);
        float yaw = entity.getYRot();
        for (int i = 0; i < n; i++) {
            float slabYaw = yaw + (i - (n - 1) / 2f) * STEP;
            Vec3 at = SpellUtil.groundAt(level, entity.position().add(direction(slabYaw).scale(DISTANCE)), 3, 3);
            AABB box = new AABB(at.x - .6, at.y, at.z - .6, at.x + .6, at.y + 2.6, at.z + .6);
            if (!level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive).isEmpty()) {
                continue;
            }
            RuneBulwark.raise(level, entity, at, slabYaw, duration(spellLevel));
            RuneFx.stone(level, at.add(0, .2, 0), Blocks.DEEPSLATE_TILES.defaultBlockState(), 14, .5);
        }
        RuneFx.sound(level, entity.position(), SoundEvents.DEEPSLATE_TILES_BREAK, 1.6f, .5f);
        SpellUtil.cameraShake(level, entity.position(), 8, 8);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
