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
 * Diamond Dust: a dome of diamond dust glittering in every colour settles where the caster aims for four seconds: the
 * enemies in it are cut again and again, blinded by its glitter and slowed.
 */
@AutoSpellConfig
public class DiamondDustSpell extends GemSpell {
    public static final double RANGE = 24;
    public static final float RADIUS = 3.5f;
    public static final int LIFE = 92;

    public DiamondDustSpell() {
        super("diamond_dust", SpellRarity.RARE, 6, 20, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 55;
    }

    public float cut(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .25f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(cut(spellLevel, caster), 1), Utils.timeFromTicks(GemScene.DUST_EVERY, 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - 12, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimed(level, entity, RANGE);
            GemScene.spawn(server, entity, this, GemScene.Kind.DUST, at, new Vector3f(0, 1, 0), RADIUS, LIFE, cut(spellLevel, entity));
            GemFx.sigil(level, at, Gem.DIAMOND, RADIUS, LIFE);
            GemFx.chime(level, at, 1.4f, 1.9f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
