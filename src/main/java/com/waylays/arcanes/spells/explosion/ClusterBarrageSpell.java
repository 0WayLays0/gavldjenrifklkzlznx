package com.waylays.arcanes.spells.explosion;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.ClusterShell;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Cluster Barrage: a big shell climbs into the sky above the target and bursts into eight sub-charges that rain down
 * and explode in a cascade (see {@link ClusterShell}).
 */
@AutoSpellConfig
public class ClusterBarrageSpell extends AbstractSpell {
    public static final int BOMBLETS = 8;
    public static final float BOMBLET_RADIUS = 2.5f;
    public static final float RANGE = 32f;
    public static final double BURST_HEIGHT = 9;
    public static final double SHELL_SPEED = 1.1;

    private final ResourceLocation spellId = ArcanesOublies.id("cluster_barrage");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.EXPLOSION_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(16)
            .build();

    public ClusterBarrageSpell() {
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 55;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.bomblet_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.arcanes_oublies.bomblets", BOMBLETS)
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
        return Optional.of(SoundEvents.FIREWORK_ROCKET_LAUNCH);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        HitResult hit = Utils.raycastForEntity(level, entity, RANGE, true, .35f);
        Vec3 target = hit instanceof EntityHitResult entityHit ? entityHit.getEntity().position() : hit.getLocation();
        // Burst high above the target, or just under the ceiling
        double height = BURST_HEIGHT;
        Vec3 above = target.add(0, 1, 0);
        HitResult roof = level.clip(new ClipContext(above, above.add(0, BURST_HEIGHT, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (roof.getType() != HitResult.Type.MISS) {
            height = Math.max(2.5, roof.getLocation().y - target.y - 1.2);
        }
        Vec3 apex = target.add(0, height, 0);
        Vec3 start = entity.getEyePosition().add(entity.getLookAngle().scale(.8));
        Vec3 path = apex.subtract(start);
        int flight = Math.max(6, (int) Math.ceil(path.length() / SHELL_SPEED));
        ClusterShell shell = new ClusterShell(level, entity, flight);
        shell.setPos(start.x, start.y - shell.getBbHeight() * .5, start.z);
        shell.setDeltaMovement(path.scale(1.0 / flight));
        shell.setDamage(getDamage(spellLevel, entity));
        level.addFreshEntity(shell);
        SpellUtil.particles(level, ExplosionFx.sparks(), start.x, start.y, start.z, 10, .1, .1, .1, .3);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .7f;
    }
}
