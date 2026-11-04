package com.waylays.arcanes.spells.aurora;

import com.waylays.arcanes.entity.aurora.AuroraScene;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/** Aurora Glide: the caster rides a wave of the northern lights a dozen blocks forward; enemies on the way are struck, tossed and marked. */
@AutoSpellConfig
public class AuroraGlideSpell extends AuroraSpell {
    public AuroraGlideSpell() {
        super("aurora_glide", SpellRarity.UNCOMMON, 8, 10, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    public float sweep(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .7f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(sweep(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.launch"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 look = entity.getLookAngle().multiply(1, 0, 1);
            if (look.lengthSqr() < 1.0E-4) {
                Vector3f f = facing(entity);
                look = new Vec3(f.x, 0, f.z);
            }
            AuroraScene.spawn(server, entity, this, AuroraScene.Kind.GLIDE, entity.position(), look.normalize().toVector3f(), 1, AuroraScene.GLIDE_TICKS + 24,
                    sweep(spellLevel, entity)).follow(entity);
            entity.setDeltaMovement(look.normalize().scale(2.3).add(0, .3, 0));
            entity.hurtMarked = true;
            entity.fallDistance = 0;
            entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 25, 0, false, false, true));
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, .8f, 1.6f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.4f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
