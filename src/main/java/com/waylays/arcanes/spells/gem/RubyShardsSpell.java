package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.GemScene;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/** Ruby Shards: three long ruby crystals flung in a narrow fan; each burns through two enemies, setting them alight, and shatters. */
@AutoSpellConfig
public class RubyShardsSpell extends GemSpell {
    public static final int SHARDS = 3;
    public static final float SPREAD = 6;

    public RubyShardsSpell() {
        super("ruby_shards", SpellRarity.COMMON, 10, 4, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    public float shard(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .6f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.gem_shards", SHARDS, Utils.stringTruncation(shard(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.pierce", GemScene.SHARD_PIERCE),
                Component.translatable("ui.arcanes_oublies.glyph_burn"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 look = entity.getLookAngle();
            Vec3 from = entity.getEyePosition().add(0, -.2, 0).add(look.scale(.7));
            for (int k = 0; k < SHARDS; k++) {
                Vec3 d = look.yRot((k - (SHARDS - 1) * .5f) * SPREAD * Mth.DEG_TO_RAD);
                GemScene.spawn(server, entity, this, GemScene.Kind.SHARD, from, d.toVector3f(), .5f, GemScene.SHARD_FLIGHT + GemScene.SHARD_BURST + 2,
                        shard(spellLevel, entity));
            }
            GemFx.sigil(level, SpellUtil.groundAt(level, entity.position(), 2, 1), Gem.RUBY, 1.6f, 14);
            GemFx.chime(level, entity.position(), 1f, 1.5f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, .7f, 1.6f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
