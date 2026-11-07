package com.waylays.arcanes.spells.mend;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.resources.ResourceLocation;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Base of the Mending Magic spells: the school, the config, the caster's gesture. */
public abstract class MendSpell extends AbstractSpell {
    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;
    private final CastType castType;
    @Nullable
    private final AnimationHolder animation;

    /** {@code animation} null: Iron's own for the cast type. */
    protected MendSpell(String name, SpellRarity minRarity, int maxLevel, int cooldownSeconds, CastType castType, @Nullable AnimationHolder animation) {
        this.spellId = ArcanesOublies.id(name);
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(minRarity)
                .setSchoolResource(ModSchools.MEND_RESOURCE)
                .setMaxLevel(maxLevel)
                .setCooldownSeconds(cooldownSeconds)
                .build();
        this.castType = castType;
        this.animation = animation;
    }

    @Override
    public CastType getCastType() {
        return castType;
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
        return animation != null ? animation : super.getCastStartAnimation();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return animation != null ? AnimationHolder.pass() : super.getCastFinishAnimation();
    }

    public float getPower(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }

    /** The way the caster faces, flat on the ground (a unit vector). */
    public static org.joml.Vector3f facing(LivingEntity caster) {
        float yaw = caster.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
        return new org.joml.Vector3f(-net.minecraft.util.Mth.sin(yaw), 0, net.minecraft.util.Mth.cos(yaw));
    }

    /** The enemy the caster looks at within {@code range}, if any. */
    @Nullable
    public static LivingEntity aimed(Level level, LivingEntity caster, double range) {
        return SpellUtil.aimedEnemy(level, caster, range);
    }

    /** Where the caster's aim meets the ground: under the enemy they look at, else the ground they point at. */
    public static Vec3 aimedSpot(Level level, LivingEntity caster, double range) {
        return SpellUtil.aimedSpot(level, caster, range);
    }

    /**
     * The ally the caster looks at within {@code range} (anyone who is not their enemy: a friend, a pet, a villager), else
     * the caster themselves.
     */
    public static LivingEntity aimedAlly(Level level, LivingEntity caster, double range) {
        net.minecraft.world.phys.HitResult hit = io.redspace.ironsspellbooks.api.util.Utils.raycastForEntity(level, caster, (float) range, true, .8f);
        if (hit instanceof net.minecraft.world.phys.EntityHitResult eh && eh.getEntity() instanceof LivingEntity living && living.isAlive()
                && !(living instanceof net.minecraft.world.entity.decoration.ArmorStand) && !(living instanceof net.minecraft.world.entity.monster.Enemy)
                && (SpellUtil.isAlly(caster, living) || !(living instanceof net.minecraft.world.entity.Mob mob) || mob.getTarget() != caster)) {
            return living;
        }
        return caster;
    }
}
