package com.waylays.arcanes.spells.wood;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.wood.WoodGuardian;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.SummoningSpell;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Sylvan Guardian: the caster calls on the old wood and a treant pulls itself out of the ground in front of them to
 * fight at their side (see WoodGuardian). Cast again to send it back into the earth.
 */
@AutoSpellConfig
public class WoodGuardianSpell extends SummoningSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("wood_guardian");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(ModSchools.WOOD_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(60)
            .build();

    public WoodGuardianSpell() {
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 2;
        this.castTime = 20;
        this.baseManaCost = 100;
    }

    public static double health(int spellLevel) {
        return 50 + 12 * spellLevel;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }

    @Override
    public int getLifetime(int spellLevel) {
        return 600 + 120 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getLifetime(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.fist_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.hp", (int) health(spellLevel)));
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_RAISED_HAND;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ROOTED_DIRT_BREAK);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.WOOD_PLACE);
    }

    /** While the caster calls, the ground in front of them stirs. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (entity.tickCount % 4 == 0) {
            float r = entity.getYRot() * Mth.DEG_TO_RAD;
            Vec3 at = entity.position().add(-Mth.sin(r) * 3, .2, Mth.cos(r) * 3);
            WoodFx.chips(level, at, 6, .6);
            WoodFx.life(level, at.add(0, .3, 0), 3, .5);
        }
    }

    /** The treant pulls itself out of the ground a few steps in front of the caster. */
    @Override
    protected boolean summon(Level level, int spellLevel, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            dismissSummons(serverLevel, entity);
        }
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        Vec3 spot = SpellUtil.groundAt(level, entity.position().add(-Mth.sin(yaw) * 3, 0, Mth.cos(yaw) * 3), 3, 4);
        WoodGuardian guardian = ModEntities.WOOD_GUARDIAN.get().create(level);
        if (guardian == null) {
            return false;
        }
        guardian.moveTo(spot.x, spot.y, spot.z, entity.getYRot(), 0);
        if (!level.noCollision(guardian)) {
            guardian.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), 0);
        }
        guardian.setYHeadRot(entity.getYRot());
        guardian.yBodyRot = entity.getYRot();
        guardian.yBodyRotO = entity.getYRot();
        guardian.setup(entity, getLifetime(spellLevel), health(spellLevel), getDamage(spellLevel, entity));
        if (entity instanceof Mob mob && mob.getTarget() != null) {
            guardian.setTarget(mob.getTarget());
        }
        level.addFreshEntity(guardian);
        WoodFx.sprout(level, spot, 1.6f);
        WoodFx.creak(level, spot, 1.4f);
        return true;
    }
}
