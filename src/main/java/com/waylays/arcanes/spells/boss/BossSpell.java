package com.waylays.arcanes.spells.boss;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.decoration.ArmorStand;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Base of the Boss Magic spells. Casting one draws its warnings on the ground ({@link BossTelegraph}) and the attack
 * lands when they run out ({@link #onTelegraphFire}); the caster is free to move meanwhile.
 */
public abstract class BossSpell extends AbstractSpell implements BossAttack {
    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;
    private final AnimationHolder animation;

    protected BossSpell(String name, SpellRarity minRarity, int maxLevel, int cooldownSeconds, AnimationHolder animation) {
        this.spellId = ArcanesOublies.id(name);
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(minRarity)
                .setSchoolResource(ModSchools.BOSS_RESOURCE)
                .setMaxLevel(maxLevel)
                .setCooldownSeconds(cooldownSeconds)
                .build();
        this.animation = animation;
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
    public AnimationHolder getCastStartAnimation() {
        return animation;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.EVOKER_CAST_SPELL);
    }

    /** "Lands after X s" for the spell's tooltip. */
    protected static MutableComponent warningInfo() {
        return Component.translatable("ui.arcanes_oublies.warning_time", Utils.stringTruncation(BossTelegraph.warningTicks() / 20f, 1));
    }

    /** The ground the caster is aiming at, at most {@code range} away (straight down from the end of the aim if it is in the air). */
    protected static Vec3 aimedGround(Level level, LivingEntity caster, double range) {
        return SpellUtil.aimedGround(level, caster, range);
    }

    /** The ground under {@code entity}. */
    protected static Vec3 feet(LivingEntity entity) {
        return SpellUtil.groundAt(entity.level(), entity.position(), 3, 1);
    }

    /** The horizontal direction the caster faces, as a yaw. */
    protected static float facing(LivingEntity caster) {
        return caster.getYRot();
    }

    protected static Vec3 direction(float yaw) {
        float r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    protected static int now(Level level) {
        return (int) level.getGameTime();
    }

    /** Knocks {@code target} away from {@code from} (horizontally), and up. */
    protected static void knockAway(LivingEntity target, Vec3 from, double strength, double up) {
        Vec3 d = target.position().subtract(from).multiply(1, 0, 1);
        d = d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 0) : d.normalize().scale(strength);
        double resist = Math.max(0, 1 - target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
        target.setDeltaMovement(target.getDeltaMovement().add(d.scale(resist)).add(0, up * resist, 0));
        target.hurtMarked = true;
    }

    /**
     * Up to {@code max} enemies of the caster within {@code range} of {@code around}, nearest first (no armour
     * stands).
     */
    protected static List<LivingEntity> foes(Level level, LivingEntity caster, Vec3 around, double range, int max) {
        List<LivingEntity> found = SpellUtil.enemiesAround(level, around, range, caster, e -> !(e instanceof ArmorStand));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(around)));
        return found.size() > max ? new ArrayList<>(found.subList(0, max)) : found;
    }

    /** Minecraft's yaw (degrees) for the horizontal direction {@code (dx, dz)}. */
    protected static float yawOf(double dx, double dz) {
        return (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }
}
