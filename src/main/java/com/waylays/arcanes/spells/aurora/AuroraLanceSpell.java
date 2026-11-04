package com.waylays.arcanes.spells.aurora;

import com.waylays.arcanes.entity.aurora.AuroraScene;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/** Aurora Lance: a spear of light tipped with ice, its tail of aurora twisting behind it: it goes through three enemies, slowing them. */
@AutoSpellConfig
public class AuroraLanceSpell extends AuroraSpell {
    public AuroraLanceSpell() {
        super("aurora_lance", SpellRarity.COMMON, 10, 4, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    public float lance(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .7f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(lance(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.pierce", AuroraScene.LANCE_PIERCE),
                Component.translatable("ui.arcanes_oublies.hamstring"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 look = entity.getLookAngle();
            AuroraScene.spawn(server, entity, this, AuroraScene.Kind.LANCE, entity.getEyePosition().add(0, -.2, 0).add(look.scale(.8)), look.toVector3f(), .5f,
                    AuroraScene.LANCE_FLIGHT + AuroraScene.BURST + 2, lance(spellLevel, entity));
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1f, 1.6f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.8f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
