package com.waylays.arcanes.spells.lotus;

import com.waylays.arcanes.entity.lotus.LotusScene;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Lotus Lanterns: lanterns shaped as lotus flowers, a flame in each, rise round the caster, then drift off each after
 * its own enemy and burst on it in a flower of fire.
 */
@AutoSpellConfig
public class LotusLanternsSpell extends LotusSpell {
    public static final float BLAST = 2.5f;
    public static final int LIFE = 200;

    public LotusLanternsSpell() {
        super("lotus_lanterns", SpellRarity.EPIC, 5, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 90;
    }

    public static int lanterns(int spellLevel) {
        return 5 + (spellLevel - 1) / 2;
    }

    public float blast(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .5f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.lanterns", lanterns(spellLevel), Utils.stringTruncation(blast(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.glyph_burn"),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(BLAST, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            int n = lanterns(spellLevel);
            for (int k = 0; k < n; k++) {
                float a = entity.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD + k * net.minecraft.util.Mth.TWO_PI / n;
                Vector3f out = new Vector3f(-net.minecraft.util.Mth.sin(a), 0, net.minecraft.util.Mth.cos(a));
                Vec3 at = entity.position().add(out.x * 1.3, 1.1, out.z * 1.3);
                LotusScene.spawn(server, entity, this, LotusScene.Kind.LANTERN, at, out, BLAST, LIFE + k * 4, blast(spellLevel, entity)).index(k);
            }
            LotusFx.pollen(level, entity.position().add(0, 1.2, 0), 16, .8);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, .8f, 1.5f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, .9f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
