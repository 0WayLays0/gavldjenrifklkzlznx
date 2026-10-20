package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.AurumBladeEntity;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Aurum Blade: a giant golden sword forms in the sky above the target and plunges into it.
 */
@AutoSpellConfig
public class AurumBladeSpell extends AbstractSpell {
    public static final float RADIUS = 2.8f;
    public static final float SPLASH_RATIO = .6f;

    private final ResourceLocation spellId = ArcanesOublies.id("aurum_blade");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GOLD_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(15)
            .build();

    public AurumBladeSpell() {
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
        this.castTime = 20;
        this.baseManaCost = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1))
        );
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ARMOR_EQUIP_GOLD.value());
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        SpellUtil.particles(level, GoldFx.sparks(), entity.getX(), entity.getY() + 2.2, entity.getZ(), 1, .15, .1, .15, .2);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        HitResult hit = Utils.raycastForEntity(level, entity, 24, true, .35f);
        Vec3 at = hit instanceof EntityHitResult entityHit ? entityHit.getEntity().position() : hit.getLocation();
        Vec3 ground = SpellUtil.groundAt(level, at, 4, 1);
        AurumBladeEntity blade = new AurumBladeEntity(level, entity, getDamage(spellLevel, entity));
        blade.moveTo(ground.x, ground.y, ground.z, entity.getYRot(), 0);
        level.addFreshEntity(blade);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }
}
