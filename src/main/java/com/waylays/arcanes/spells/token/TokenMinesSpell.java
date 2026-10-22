package com.waylays.arcanes.spells.token;

import com.waylays.arcanes.entity.token.TokenMine;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Trap Tokens: a fan of soul tokens scattered on the ground ahead. Once armed, each bursts when an enemy comes near,
 * hurting and slowing those around.
 */
@AutoSpellConfig
public class TokenMinesSpell extends TokenSpell {
    public TokenMinesSpell() {
        super("token_mines", SpellRarity.UNCOMMON, 8, 12, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 30;
    }

    public static int tokens(int spellLevel) {
        return 3 + spellLevel / 3;
    }

    public float damage(int spellLevel, LivingEntity caster) {
        return getPower(spellLevel, caster) * 1.1f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.token_count", tokens(spellLevel)),
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(damage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(TokenMine.BURST, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            int n = tokens(spellLevel);
            float damage = damage(spellLevel, entity);
            for (int i = 0; i < n; i++) {
                // A fan ahead, staggered in depth so they do not burst all together
                float yaw = entity.getYRot() + (n == 1 ? 0 : (i / (n - 1f) - .5f) * 70);
                double distance = 2.5 + (i % 2) * 1.5 + serverLevel.random.nextDouble() * .5;
                Vec3 dir = Vec3.directionFromRotation(0, yaw);
                Vec3 at = SpellUtil.groundAt(level, entity.position().add(dir.scale(distance)), 3, 1);
                TokenMine.place(serverLevel, entity, this, at, damage);
            }
            TokenFx.clink(level, entity.position(), 1f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
