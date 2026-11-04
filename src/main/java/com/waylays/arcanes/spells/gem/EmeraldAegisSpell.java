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
 * Emerald Aegis: plates of emerald fly in from all round and close into a shell round the caster, then sink into them:
 * the caster and their side close by are shielded (Absorption) for fifteen seconds and regenerate a while.
 */
@AutoSpellConfig
public class EmeraldAegisSpell extends GemSpell {
    public static final double RANGE = 6;
    public static final int SHIELD = 300;

    public EmeraldAegisSpell() {
        super("emerald_aegis", SpellRarity.UNCOMMON, 8, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    public static int amplifier(int spellLevel) {
        return 1 + (spellLevel - 1) / 2;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.aegis_absorb", (amplifier(spellLevel) + 1) * 4, Utils.stringTruncation(RANGE, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(SHIELD, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(entity.position(), entity.position()).inflate(RANGE))) {
                if (e.isAlive() && (e == entity || SpellUtil.isAlly(entity, e) || e.isAlliedTo(entity)) && e.distanceTo(entity) <= RANGE) {
                    e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, SHIELD, amplifier(spellLevel), false, true, true));
                    e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true, true));
                    GemFx.glitter(level, e.position().add(0, 1, 0), Gem.EMERALD, 12, .5);
                    if (e != entity) {
                        GemScene.spawn(server, entity, this, GemScene.Kind.AEGIS, e.position(), new Vector3f(0, 0, 1), 1, 60, 0).follow(e);
                    }
                }
            }
            GemScene.spawn(server, entity, this, GemScene.Kind.AEGIS, entity.position(), new Vector3f(0, 0, 1), 1, 60, 0).follow(entity);
            GemFx.sigil(level, SpellUtil.groundAt(level, entity.position(), 2, 1), Gem.EMERALD, 2, 20);
            GemFx.chime(level, entity.position(), 1.4f, 1.1f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS, 1.2f, 1.4f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
