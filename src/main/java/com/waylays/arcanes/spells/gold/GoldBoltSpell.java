package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GoldBoltProjectile;
import com.waylays.arcanes.registry.ModSchools;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Gold Bolt: a spinning golden ingot that bursts into gold shards on impact. The direct hit takes full damage,
 * enemies around the impact take {@link #SPLASH_RATIO} of it.
 */
@AutoSpellConfig
public class GoldBoltSpell extends AbstractSpell {
    public static final float SPLASH_RATIO = .4f;
    public static final float SPLASH_RADIUS = 2.5f;

    private final ResourceLocation spellId = ArcanesOublies.id("gold_bolt");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.GOLD_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(1.5)
            .build();

    public GoldBoltSpell() {
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float damage = getDamage(spellLevel, caster);
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage, 2)),
                Component.translatable("ui.arcanes_oublies.splash_damage", Utils.stringTruncation(damage * SPLASH_RATIO, 2))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
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
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ARMOR_EQUIP_GOLD);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        GoldBoltProjectile bolt = new GoldBoltProjectile(level, entity);
        Vec3 look = entity.getLookAngle();
        Vec3 origin = entity.getEyePosition().add(look.scale(.4)).subtract(0, bolt.getBbHeight() * .5f, 0);
        bolt.setPos(origin);
        bolt.shoot(look);
        bolt.setDamage(getDamage(spellLevel, entity));
        level.addFreshEntity(bolt);

        Vec3 hand = entity.getEyePosition().add(look.scale(.8)).subtract(0, .2, 0);
        GoldFx.particles(level, GoldFx.sparks(), hand.x, hand.y, hand.z, 6, .05, .05, .05, .2);
        GoldFx.particles(level, GoldFx.PALE_DUST, hand.x, hand.y, hand.z, 8, .15, .15, .15, .02);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.4f + Utils.random.nextFloat() * .2f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .6f;
    }
}
