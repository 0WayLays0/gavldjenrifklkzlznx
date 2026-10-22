package com.waylays.arcanes.spells.token;

import com.waylays.arcanes.entity.token.TokenBolt;
import com.waylays.arcanes.entity.token.TokenFlip;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.boss.BossScheduler;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Coin Flip: the caster tosses a soul token and lets it decide. Heads: the token flies at the enemy they look at and
 * hits it twice as hard (or, with no one in sight, Strength for 10 seconds). Tails: it heals the caster and wraps them
 * in Absorption.
 */
@AutoSpellConfig
public class CoinFlipSpell extends TokenSpell {
    public static final float RANGE = 24;

    public CoinFlipSpell() {
        super("coin_flip", SpellRarity.UNCOMMON, 8, 10, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    public float strike(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * 2;
    }

    public float heal(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * .8f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.coin_heads", Utils.stringTruncation(strike(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.coin_tails", Utils.stringTruncation(heal(spellLevel, caster), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            TokenFlip flip = TokenFlip.toss(level, entity);
            TokenFx.sound(level, entity.position(), SoundEvents.CHAIN_PLACE, 1f, 2f);
            boolean heads = serverLevel.random.nextBoolean();
            float strike = strike(spellLevel, entity);
            float heal = heal(spellLevel, entity);
            BossScheduler.after(serverLevel, TokenFlip.TOSS_TICKS, l -> land(l, entity, flip, heads, strike, heal));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void land(ServerLevel level, LivingEntity caster, TokenFlip flip, boolean heads, float strike, float heal) {
        if (!caster.isAlive()) {
            return;
        }
        flip.setResult(heads ? TokenFlip.HEADS : TokenFlip.TAILS);
        Vec3 hand = caster.position().add(TokenFlip.hand(caster));
        if (heads) {
            LivingEntity target = aimed(level, caster);
            if (target != null) {
                TokenBolt.shoot(level, caster, this, TokenBolt.Mode.STRIKE, hand, TokenFx.chest(target).subtract(hand), 2f, strike).withSize(1.4f);
            } else {
                caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
            }
            TokenFx.sound(level, hand, SoundEvents.PLAYER_LEVELUP, .6f, 1.6f);
        } else {
            caster.heal(heal);
            caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
            TokenFx.burst(level, TokenFx.chest(caster), 14, .5);
            TokenFx.sound(level, hand, SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, .9f);
        }
        if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(heads ? "ui.arcanes_oublies.coin_heads_landed" : "ui.arcanes_oublies.coin_tails_landed")
                    .withStyle(heads ? ChatFormatting.GOLD : ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        }
    }

    /** The enemy the caster looks at, within range. */
    @Nullable
    private static LivingEntity aimed(ServerLevel level, LivingEntity caster) {
        HitResult hit = Utils.raycastForEntity(level, caster, RANGE, true, .6f);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living && living.isAlive()
                && !(living instanceof ArmorStand) && !SpellUtil.isAlly(caster, living)) {
            return living;
        }
        return null;
    }
}
