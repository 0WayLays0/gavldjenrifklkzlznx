package com.waylays.arcanes.spells.rune;

import com.waylays.arcanes.entity.rune.RuneShard;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Rune Shards: a fan of carved stone shards hurled where the caster looks; each one goes through an enemy. */
@AutoSpellConfig
public class RuneShardsSpell extends RuneSpell {
    public static final float SPEED = 1.7f;
    /** Degrees between two shards of the fan. */
    public static final float SPREAD = 7;
    public static final int PIERCE = 1;

    public RuneShardsSpell() {
        super("rune_shards", SpellRarity.COMMON, 5, 6, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    public static int shards(int spellLevel) {
        return 3 + (spellLevel - 1) / 2;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.shards", shards(spellLevel), Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.pierce", PIERCE));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.DEEPSLATE_BRICKS_BREAK);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int n = shards(spellLevel);
        float damage = getPower(spellLevel, entity);
        Vec3 origin = entity.getEyePosition().add(0, -.25, 0).add(entity.getLookAngle().scale(.6));
        for (int i = 0; i < n; i++) {
            Vec3 dir = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot() + (i - (n - 1) / 2f) * SPREAD);
            RuneShard shard = new RuneShard(level, entity, damage, PIERCE);
            shard.setPos(origin.x, origin.y - shard.getBbHeight() * .5, origin.z);
            shard.shoot(dir, SPEED);
            level.addFreshEntity(shard);
        }
        RuneFx.glyphs(level, origin, 6, .2);
        RuneFx.sound(level, origin, SoundEvents.AMETHYST_BLOCK_RESONATE, .8f, 1.8f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
