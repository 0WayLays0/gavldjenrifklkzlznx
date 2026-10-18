package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GoldfallIngot;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Goldfall: massive ingots rain from the sky around the targeted spot, one after the other.
 */
@AutoSpellConfig
public class GoldfallSpell extends AbstractSpell {
    public static final float IMPACT_RADIUS = 2.2f;
    public static final float AREA_RADIUS = 3.5f;

    private final ResourceLocation spellId = ArcanesOublies.id("goldfall");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.GOLD_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(18)
            .build();

    public GoldfallSpell() {
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 30;
        this.baseManaCost = 60;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.arcanes_oublies.ingot_count", getIngotCount(spellLevel))
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
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ARMOR_EQUIP_GOLD);
    }

    /** While charging: gold dust rises from the caster's hands toward the sky. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        SpellUtil.particles(level, GoldFx.GOLD_DUST, entity.getX(), entity.getY() + 1.2, entity.getZ(), 3, .4, .2, .4, 0);
        SpellUtil.particles(level, GoldFx.sparks(), entity.getX(), entity.getY() + 2, entity.getZ(), 1, .2, .2, .2, .3);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 center = findTarget(level, entity);
        int count = getIngotCount(spellLevel);
        float damage = getDamage(spellLevel, entity);
        for (int i = 0; i < count; i++) {
            double angle = i * 2.4; // golden angle spiral: evenly spread, never two in the same spot
            double radius = AREA_RADIUS * Math.sqrt((i + .5) / count);
            Vec3 ground = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            // Staggered heights: the ingots land one after the other. Under a roof they start just below it.
            double height = 14 + i * 2.5;
            Vec3 above = ground.add(0, 1, 0);
            HitResult roof = level.clip(new ClipContext(above, above.add(0, height, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
            if (roof.getType() != HitResult.Type.MISS) {
                height = Math.max(1.5, roof.getLocation().y - ground.y - 1.2);
            }
            GoldfallIngot ingot = new GoldfallIngot(level, entity);
            ingot.setPos(ground.x + Mth.nextDouble(entity.getRandom(), -.8, .8), ground.y + height, ground.z + Mth.nextDouble(entity.getRandom(), -.8, .8));
            ingot.setDeltaMovement(0, -.4, 0);
            ingot.setDamage(damage);
            level.addFreshEntity(ingot);
        }
        SpellUtil.particles(level, GoldFx.sparks(), center.x, center.y + .2, center.z, 20, AREA_RADIUS * .5, .1, AREA_RADIUS * .5, .1);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** The entity or block the caster looks at, up to 32 blocks away. */
    private static Vec3 findTarget(Level level, LivingEntity caster) {
        HitResult hit = Utils.raycastForEntity(level, caster, 32, true, .35f);
        if (hit instanceof EntityHitResult entityHit) {
            return entityHit.getEntity().position();
        }
        return hit.getLocation();
    }

    public int getIngotCount(int spellLevel) {
        return 4 + spellLevel / 2;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .8f;
    }
}
