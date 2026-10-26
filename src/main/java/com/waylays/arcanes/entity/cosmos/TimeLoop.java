package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import com.waylays.arcanes.spells.cosmos.CosmosFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * A time loop closed round one enemy: every three seconds it is snapped back to where it stood when the loop closed
 * (an echo of it is left where it had got to), and the blows it took during the loop strike it again, worse each time
 * round. A golden clock ring turns round it, its hand sweeping the loop; when the loops are spent it lets go.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class TimeLoop extends SpellEffectEntity {
    public static final int PERIOD = 60;
    /** Share of the damage taken in a loop that strikes again when it closes. */
    public static final float ECHO = .6f;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(TimeLoop.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LOOPS = SynchedEntityData.defineId(TimeLoop.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private Vec3 anchor = Vec3.ZERO;
    private float anchorYRot;
    /** Damage the target took during the current loop. */
    private float taken;

    public TimeLoop(EntityType<? extends TimeLoop> entityType, Level level) {
        super(entityType, level);
    }

    public static void close(Level level, LivingEntity caster, LivingEntity target, AbstractSpell spell, int loops, float damage) {
        for (TimeLoop old : level.getEntitiesOfClass(TimeLoop.class, target.getBoundingBox().inflate(4), l -> l.getTarget() == target)) {
            old.discard();
        }
        TimeLoop loop = new TimeLoop(ModEntities.TIME_LOOP.get(), level);
        loop.setOwner(caster);
        loop.spellId = spell.getSpellId();
        loop.damage = damage;
        loop.anchor = target.position();
        loop.anchorYRot = target.getYRot();
        loop.moveTo(target.getX(), target.getY(), target.getZ(), 0, 0);
        loop.entityData.set(DATA_TARGET, target.getId());
        loop.entityData.set(DATA_LOOPS, loops);
        level.addFreshEntity(loop);
        CosmosFx.fold(level, target.position().add(0, 1, 0), 1, .7f);
        CosmosFx.time(level, target.position().add(0, 1, 0), 30, .6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_LOOPS, 3);
    }

    @Nullable
    public LivingEntity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET)) instanceof LivingEntity living ? living : null;
    }

    public int getLoops() {
        return this.entityData.get(DATA_LOOPS);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity target = getTarget();
        if (target != null) {
            setPos(target.getX(), target.getY(), target.getZ());
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (target == null || !target.isAlive() || tickCount > getLoops() * PERIOD + 2) {
            discard();
            return;
        }
        if (tickCount % 20 == 0) {
            CosmosFx.tick(level, target.position(), .8f, 1.6f);
        }
        if (tickCount % PERIOD == 0) {
            loopBack(level, target);
        }
    }

    private void loopBack(ServerLevel level, LivingEntity target) {
        // An echo where it had got to; then back to the start
        TemporalEcho.leave(level, target, target.position(), target.getYRot(), 30, Cosmos.ORANGE);
        target.teleportTo(level, anchor.x, anchor.y, anchor.z, Set.of(), anchorYRot, target.getXRot());
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.resetFallDistance();
        CosmosFx.fold(level, anchor.add(0, 1, 0), .9f, 1.3f);
        CosmosFx.sound(level, anchor, SoundEvents.BELL_RESONATE, .7f, 1.6f);
        CosmosFx.time(level, anchor.add(0, target.getBbHeight() * .5, 0), 24, .5);
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        Entity owner = getOwner();
        if (spell != null) {
            // The loop's own sting, and the blows of the loop striking again
            float again = damage + taken * ECHO;
            taken = 0;
            target.invulnerableTime = 0;
            DamageSources.applyDamage(target, again, SpellUtil.damageSource(spell, this, owner));
        }
    }

    /** What hits the looped target is remembered, to strike again when the loop closes. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        for (TimeLoop loop : target.level().getEntitiesOfClass(TimeLoop.class, new AABB(target.position(), target.position()).inflate(2),
                l -> l.getTarget() == target)) {
            // The loop's own echo does not count again
            if (event.getSource().getDirectEntity() != loop) {
                loop.taken += event.getNewDamage();
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
