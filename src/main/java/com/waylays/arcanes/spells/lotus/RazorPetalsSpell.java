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

/** Razor Petals: five lotus petals, keen as blades, flung in a fan; each cuts the first enemy in its way and falls apart. */
@AutoSpellConfig
public class RazorPetalsSpell extends LotusSpell {
    public static final int PETALS = 5;
    /** Degrees between two petals of the fan. */
    public static final float SPREAD = 11;

    public RazorPetalsSpell() {
        super("razor_petals", SpellRarity.COMMON, 10, 3, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    public float cut(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .6f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.petals", PETALS, Utils.stringTruncation(cut(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(LotusScene.PETAL_SPEED * LotusScene.PETAL_FLIGHT, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 look = entity.getLookAngle();
            Vec3 from = entity.getEyePosition().add(0, -.25, 0).add(look.scale(.6));
            for (int k = 0; k < PETALS; k++) {
                Vec3 d = look.yRot((k - (PETALS - 1) * .5f) * SPREAD * net.minecraft.util.Mth.DEG_TO_RAD);
                LotusScene.spawn(server, entity, this, LotusScene.Kind.PETAL, from, d.toVector3f(), .5f, LotusScene.PETAL_FLIGHT + LotusScene.PETAL_BURST + 2,
                        cut(spellLevel, entity)).index(k);
            }
            LotusFx.petals(level, from, 8, .3);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.5f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 1f, 1.3f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
