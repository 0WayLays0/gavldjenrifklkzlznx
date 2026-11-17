package com.waylays.arcanes.spells.lava;

import com.waylays.arcanes.entity.lava.LavaScene;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.ElementSpell;
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

import java.util.List;

/** Magma Spit: a gob of magma lobbed in an arc, splashing into a bubbling pool that burns whoever stands in it. */
@AutoSpellConfig
public class MagmaSpitSpell extends ElementSpell {
    public MagmaSpitSpell() {
        super("magma_spit", ModSchools.LAVA_RESOURCE, SpellRarity.COMMON, 8, 5, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power * .9f, 1)),
                Component.translatable("ui.arcanes_oublies.lava.magma_spit", Utils.timeFromTicks(LavaScene.POOL_TIME, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 target = SpellUtil.aimedSpot(level, entity, 20);
            Vec3 hand = entity.getEyePosition().add(facing(entity).scale(.6)).add(0, -.4, 0);
            LavaScene.spawn(server, entity, this, LavaScene.Kind.SPIT, hand, target, entity.getYRot(), LavaScene.SPIT_FLIGHT + 5, getPower(spellLevel, entity));
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1, .7f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
