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
 * Lotus Bud: a great bud bursts out of the ground round the enemy the caster looks at and closes on it, holding it in its
 * petals; then it bursts open, flinging its prey into the air.
 */
@AutoSpellConfig
public class LotusBudSpell extends LotusSpell {
    public static final double RANGE = 20;
    public static final float RADIUS = 1.3f;

    public LotusBudSpell() {
        super("lotus_bud", SpellRarity.COMMON, 10, 8, CastType.INSTANT, SpellAnimations.CHARGE_RAISED_HAND);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.rooted", Utils.timeFromTicks(LotusScene.BUD_BURST - LotusScene.BUD_RISE + 2, 1)),
                Component.translatable("ui.arcanes_oublies.launch"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = aimedSpot(level, entity, RANGE);
            LotusScene.spawn(server, entity, this, LotusScene.Kind.BUD, at, new Vector3f(0, 1, 0), RADIUS, LotusScene.BUD_BURST + 24, getPower(spellLevel, entity));
            LotusFx.petals(level, at.add(0, .3, 0), 8, .6);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
