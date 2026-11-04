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

/**
 * Celestial Vault: a disc of the night sky opens high over the spot the caster aims at, deep lapis set with a
 * constellation of gold, a ring of golden runes turning round it; for five seconds golden stars fall out of it onto
 * the enemies beneath.
 */
@AutoSpellConfig
public class CelestialVaultSpell extends GemSpell {
    public static final double RANGE = 28;
    public static final float RADIUS = 5;
    public static final int LIFE = GemScene.VAULT_OPEN + 100;

    public CelestialVaultSpell() {
        super("celestial_vault", SpellRarity.RARE, 6, 22, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public float star(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .3f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(star(spellLevel, caster), 1), Utils.timeFromTicks(GemScene.VAULT_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - GemScene.VAULT_OPEN - 12, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimed(level, entity, RANGE);
            GemScene.spawn(server, entity, this, GemScene.Kind.VAULT, at, new Vector3f(0, 1, 0), RADIUS, LIFE, star(spellLevel, entity));
            GemFx.sigil(level, at, Gem.LAPIS, RADIUS, LIFE);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
