package com.waylays.arcanes.spells.explosion;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.RuinSigil;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Sigils of Ruin: runes of ruin burn into the ground in an arc in front of the caster. An enemy stepping on one sets
 * it off, and each blast sets off the sigils next to it (see {@link RuinSigil}).
 */
@AutoSpellConfig
public class SigilsOfRuinSpell extends AbstractSpell {
    public static final float RADIUS = 3f;
    public static final double DISTANCE = 4;

    private final ResourceLocation spellId = ArcanesOublies.id("sigils_of_ruin");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.EXPLOSION_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(20)
            .build();

    public SigilsOfRuinSpell() {
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.arcanes_oublies.sigils", getSigilCount(spellLevel))
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
        return Optional.of(SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 look = entity.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z);
        forward = forward.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, entity.getYRot()) : forward.normalize();
        float damage = getDamage(spellLevel, entity);
        int count = getSigilCount(spellLevel);
        // An arc in front of the caster, about 2.6 blocks between sigils
        double spread = Math.toRadians(32);
        for (int i = 0; i < count; i++) {
            double angle = (i - (count - 1) * .5) * spread;
            Vec3 dir = forward.yRot((float) angle);
            Vec3 ground = SpellUtil.groundAt(level, entity.position().add(dir.scale(DISTANCE + (i % 2) * .8)), 3, 1);
            RuinSigil sigil = new RuinSigil(level, entity, damage);
            sigil.moveTo(ground.x, ground.y, ground.z);
            level.addFreshEntity(sigil);
            SpellUtil.particles(level, ExplosionFx.EMBER_DUST, ground.x, ground.y + .1, ground.z, 10, .6, .05, .6, 0);
            SpellUtil.particles(level, ExplosionFx.sparks(), ground.x, ground.y + .1, ground.z, 4, .3, .05, .3, .15);
        }
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, .8f, .6f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 3 sigils at level 1, up to 5. */
    public int getSigilCount(int spellLevel) {
        return Math.min(5, 3 + (spellLevel - 1) / 3);
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }
}
