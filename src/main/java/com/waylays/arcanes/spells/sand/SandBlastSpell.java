package com.waylays.arcanes.spells.sand;

import com.waylays.arcanes.entity.sand.SandScene;
import com.waylays.arcanes.entity.sand.GlassShard;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/** Sand Blast: a gout of sand flung over a cone ahead, hurting and blinding for a moment whatever it catches, pushing it back. */
@AutoSpellConfig
public class SandBlastSpell extends SandSpell {
    public static final float LENGTH = 6;
    public static final float HALF_ANGLE = 30;

    public SandBlastSpell() {
        super("sand_blast", SpellRarity.COMMON, 10, 3, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.reach", Utils.stringTruncation(LENGTH, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 eye = entity.getEyePosition();
            Vec3 look = entity.getLookAngle();
            float damage = getPower(spellLevel, entity);
            double cos = Math.cos(Math.toRadians(HALF_ANGLE));
            for (LivingEntity e : SpellUtil.enemiesAround(level, eye, LENGTH, entity, x -> true)) {
                Vec3 to = e.position().add(0, e.getBbHeight() * .5, 0).subtract(eye);
                if (to.lengthSqr() < 1.0E-4 || to.normalize().dot(look) >= cos) {
                    e.invulnerableTime = 0;
                    if (io.redspace.ironsspellbooks.damage.DamageSources.applyDamage(e, damage, SpellUtil.damageSource(this, entity, entity))) {
                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                        Vec3 push = to.multiply(1, 0, 1);
                        if (push.lengthSqr() > 1.0E-4) {
                            e.setDeltaMovement(e.getDeltaMovement().add(push.normalize().scale(.35)).add(0, .1, 0));
                            e.hurtMarked = true;
                        }
                    }
                }
            }
            SandScene.spawn(server, entity, this, SandScene.Kind.BLAST, eye.add(look.scale(.4)).subtract(0, .2, 0), look.toVector3f(), LENGTH, 16, 0);
            SandFx.sand(level, eye.add(look.scale(2.5)), 20, .8);
            level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.4f, .8f);
            level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, .8f, .7f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
