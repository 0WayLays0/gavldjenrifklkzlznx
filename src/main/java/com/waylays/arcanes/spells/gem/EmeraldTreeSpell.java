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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Emerald Tree: an emerald thrown where the caster aims takes root and grows into a tree of crystal, emerald fruits
 * hanging from it; for twelve seconds, every two seconds, a fruit falls and heals the caster's side round it.
 */
@AutoSpellConfig
public class EmeraldTreeSpell extends GemSpell {
    public static final double RANGE = 16;
    public static final float RADIUS = 5;
    public static final int LIFE = GemScene.TREE_GROW + 240 + 15;

    public EmeraldTreeSpell() {
        super("emerald_tree", SpellRarity.RARE, 6, 35, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public float heal(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .6f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.heal_per_pulse", Utils.stringTruncation(heal(spellLevel, caster), 1), Utils.timeFromTicks(GemScene.TREE_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - GemScene.TREE_GROW - 15, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = SpellUtil.aimedGround(level, entity, RANGE);
            GemScene.spawn(server, entity, this, GemScene.Kind.TREE, at, new Vector3f(0, 1, 0), RADIUS, LIFE, heal(spellLevel, entity));
            GemFx.sigil(level, at, Gem.EMERALD, RADIUS, 30);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
