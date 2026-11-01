package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GoldSigil;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Goldfall: the sun stone opens face down in the sky over the targeted spot, giant ingots of gold gather under it and
 * drop one after the other, point first. Each one crushes and throws back what is around where it lands, stays
 * planted there for a moment and melts away into coins (see {@link GoldfallIngot}).
 */
@AutoSpellConfig
public class GoldfallSpell extends AbstractSpell {
    public static final float IMPACT_RADIUS = 2.2f;
    public static final float AREA_RADIUS = 3.5f;
    /** Height of the vault over the targeted spot, under the open sky. */
    public static final double VAULT_HEIGHT = 12;
    /** Ticks between two ingots dropping. */
    public static final int DROP_EVERY = 5;

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
        return Optional.of(SoundEvents.ARMOR_EQUIP_GOLD.value());
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
        // Under a roof the vault opens just below it
        double vault = VAULT_HEIGHT;
        Vec3 up = center.add(0, 1, 0);
        HitResult ceiling = level.clip(new ClipContext(up, up.add(0, VAULT_HEIGHT, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (ceiling.getType() != HitResult.Type.MISS) {
            vault = Math.max(3, ceiling.getLocation().y - center.y - .3);
        }
        int firstDrop = 8;
        GoldSigil.vault(level, center.add(0, vault, 0), AREA_RADIUS + 1.2f, firstDrop + count * DROP_EVERY + 20);
        GoldFx.sigil(level, center, AREA_RADIUS, firstDrop + count * DROP_EVERY + 30);
        for (int i = 0; i < count; i++) {
            double angle = i * 2.4; // golden angle spiral: evenly spread, never two in the same spot
            double radius = AREA_RADIUS * Math.sqrt((i + .5) / count);
            Vec3 ground = center.add(Math.cos(angle) * radius + Mth.nextDouble(entity.getRandom(), -.6, .6), 0,
                    Math.sin(angle) * radius + Mth.nextDouble(entity.getRandom(), -.6, .6));
            // The ingots hang just under the vault, or under whatever roof is lower over their spot
            double height = vault - 1.6;
            Vec3 above = ground.add(0, 1, 0);
            HitResult roof = level.clip(new ClipContext(above, above.add(0, height + 1.6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (roof.getType() != HitResult.Type.MISS) {
                height = Math.max(1.5, roof.getLocation().y - ground.y - 1.6);
            }
            GoldfallIngot ingot = new GoldfallIngot(level, entity);
            ingot.setPos(ground.x, ground.y + height, ground.z);
            ingot.setWait(firstDrop + i * DROP_EVERY);
            ingot.setDamage(damage);
            level.addFreshEntity(ingot);
        }
        SpellUtil.particles(level, GoldFx.sparks(), center.x, center.y + vault, center.z, 30, AREA_RADIUS * .6, .2, AREA_RADIUS * .6, .1);
        level.playSound(null, center.x, center.y + vault, center.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 3f, .7f);
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
