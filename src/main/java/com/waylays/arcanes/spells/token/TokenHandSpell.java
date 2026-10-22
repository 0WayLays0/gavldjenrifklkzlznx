package com.waylays.arcanes.spells.token;

import com.waylays.arcanes.entity.token.TokenHand;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Hand of Souls (ultimate): dozens of soul tokens fly in from every side and come together into a giant hand over the
 * enemy the caster looks at. It seizes it, lifts it, crushing it, and slams it into the ground, throwing everyone
 * around. With no one in sight, the hand slams the spot the caster points at.
 */
@AutoSpellConfig
public class TokenHandSpell extends TokenSpell {
    public static final float RANGE = 24;

    public TokenHandSpell() {
        super("token_hand", SpellRarity.LEGENDARY, 3, 120, CastType.LONG, null);
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 30;
        this.baseManaCost = 250;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.arcanes_oublies.hand_crush", Utils.stringTruncation(power * .35f, 1)),
                Component.translatable("ui.arcanes_oublies.hand_slam", Utils.stringTruncation(power * 2, 1)),
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(power * .8f, 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_AMBIENT);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    /** While it is cast, soul tokens gather around the caster, spiralling up. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .5f;
        for (int i = 0; i < 4; i++) {
            float b = a + i * Mth.HALF_PI;
            SpellUtil.particles(level, TokenFx.DUST, entity.getX() + Mth.cos(b) * 1.5, entity.getY() + (entity.tickCount % 20) * .13, entity.getZ() + Mth.sin(b) * 1.5, 1, 0, 0, 0, 0);
        }
        if (entity.tickCount % 5 == 0) {
            TokenFx.clink(level, entity.position(), .5f);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        LivingEntity target = aimed(level, entity);
        Vec3 spot = target != null ? target.position() : SpellUtil.aimedGround(level, entity, RANGE);
        TokenHand.summon(level, entity, this, target, spot, getPower(spellLevel, entity));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Nullable
    private static LivingEntity aimed(Level level, LivingEntity caster) {
        HitResult hit = Utils.raycastForEntity(level, caster, RANGE, true, .8f);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living && living.isAlive()
                && !(living instanceof ArmorStand) && !SpellUtil.isAlly(caster, living)) {
            return living;
        }
        return null;
    }
}
