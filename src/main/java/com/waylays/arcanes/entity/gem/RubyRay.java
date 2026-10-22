package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gem.Gem;
import com.waylays.arcanes.spells.gem.GemFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Ruby Ray: a ruby hovers before the caster's hand and pours out a beam of red light where they look, as long as
 * they hold the spell. The beam goes through everything living on its way, burning it, and scorches where it ends. A
 * Diamond Prism of the caster's on its way catches it and splits it into three beams that seek the enemies around.
 */
public class RubyRay extends SpellEffectEntity {
    public static final float RANGE = 24;
    public static final int HIT_TICKS = 4;
    public static final float SPLIT_RANGE = 12;
    public static final int MAX_SPLITS = 3;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(RubyRay.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_END = SynchedEntityData.defineId(RubyRay.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_SPLITS = SynchedEntityData.defineId(RubyRay.class, EntityDataSerializers.INT);
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Vector3f>[] DATA_SPLIT = new EntityDataAccessor[MAX_SPLITS];

    static {
        for (int i = 0; i < MAX_SPLITS; i++) {
            DATA_SPLIT[i] = SynchedEntityData.defineId(RubyRay.class, EntityDataSerializers.VECTOR3);
        }
    }

    @Nullable
    private String spellId;
    private int heldUntil;

    public RubyRay(EntityType<? extends RubyRay> entityType, Level level) {
        super(entityType, level);
    }

    /** Keeps the caster's ray going a few more ticks (starting it if there is none). */
    public static RubyRay hold(Level level, LivingEntity caster, AbstractSpell spell, float damage) {
        for (RubyRay ray : level.getEntitiesOfClass(RubyRay.class, caster.getBoundingBox().inflate(4),
                r -> !r.isRemoved() && caster.getUUID().equals(r.getOwnerUUID()))) {
            ray.heldUntil = ray.tickCount + 4;
            ray.damage = damage;
            return ray;
        }
        RubyRay ray = new RubyRay(ModEntities.RUBY_RAY.get(), level);
        ray.setOwner(caster);
        ray.spellId = spell.getSpellId();
        ray.damage = damage;
        ray.heldUntil = 4;
        Vec3 from = origin(caster, 1);
        ray.moveTo(from.x, from.y, from.z, 0, 0);
        ray.entityData.set(DATA_OWNER, caster.getId());
        ray.entityData.set(DATA_END, from.toVector3f());
        level.addFreshEntity(ray);
        GemFx.chime(level, from, 1.2f, .7f);
        return ray;
    }

    /** Where the beam starts: the ruby before the caster's hand. */
    public static Vec3 origin(Entity caster, float partialTicks) {
        Vec3 look = caster.getViewVector(partialTicks);
        return caster.getEyePosition(partialTicks).add(look.scale(.9)).add(0, -.3, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_END, new Vector3f());
        builder.define(DATA_SPLITS, 0);
        for (EntityDataAccessor<Vector3f> split : DATA_SPLIT) {
            builder.define(split, new Vector3f());
        }
    }

    /** Client and server: the caster, if it is around. */
    @Nullable
    public Entity getCaster() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    public Vec3 getEnd() {
        return new Vec3(this.entityData.get(DATA_END));
    }

    /** The ends of the beams a prism split this one into. */
    public List<Vec3> getSplits() {
        List<Vec3> out = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_SPLITS, this.entityData.get(DATA_SPLITS)); i++) {
            out.add(new Vec3(this.entityData.get(DATA_SPLIT[i])));
        }
        return out;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity owner = getLivingOwner();
        if (owner == null || !owner.isAlive() || tickCount > heldUntil) {
            discard();
            return;
        }
        Vec3 from = origin(owner, 1);
        Vec3 look = owner.getLookAngle();
        setPos(from.x, from.y, from.z);
        Vec3 to = from.add(look.scale(RANGE));
        BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        // A prism of the caster's on the way catches the beam
        DiamondPrism prism = null;
        double nearest = from.distanceTo(end);
        for (DiamondPrism p : level.getEntitiesOfClass(DiamondPrism.class, new AABB(from, end).inflate(DiamondPrism.CATCH + 1),
                p -> owner.getUUID().equals(p.getOwnerUUID()) && !p.isRemoved())) {
            Vec3 c = p.center();
            double along = c.subtract(from).dot(look);
            if (along > 0 && along < nearest && c.distanceTo(from.add(look.scale(along))) < DiamondPrism.CATCH) {
                nearest = along;
                prism = p;
            }
        }
        if (prism != null) {
            end = prism.center();
        }
        this.entityData.set(DATA_END, end.toVector3f());
        boolean hit = tickCount % HIT_TICKS == 0;
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (hit && spell != null) {
            burn(level, owner, spell, from, end, damage);
        }
        List<Vec3> splits = prism == null ? List.of() : split(level, owner, prism, look);
        this.entityData.set(DATA_SPLITS, splits.size());
        for (int i = 0; i < splits.size(); i++) {
            this.entityData.set(DATA_SPLIT[i], splits.get(i).toVector3f());
            if (hit && spell != null) {
                burn(level, owner, spell, end, splits.get(i), damage * .8f);
            }
        }
        if (prism != null) {
            prism.catchRay();
        } else if (block.getType() != HitResult.Type.MISS && tickCount % 2 == 0) {
            // Where it ends, the ground glows and smokes
            SpellUtil.particles(level, ParticleTypes.LAVA, end.x, end.y, end.z, 1, .1, .05, .1, 0);
            SpellUtil.particles(level, ParticleTypes.SMOKE, end.x, end.y + .1, end.z, 2, .15, .05, .15, .02);
            SpellUtil.particles(level, Gem.RUBY.dust, end.x, end.y + .1, end.z, 3, .2, .1, .2, .05);
        }
        if (tickCount % 12 == 0) {
            GemFx.sound(level, from, SoundEvents.BEACON_AMBIENT, 1.2f, 1.9f);
            GemFx.sound(level, end, SoundEvents.FIRE_AMBIENT, .8f, 1.4f);
        }
    }

    /** The three beams out of the prism: at the enemies nearest it (in sight), else fanned out ahead. */
    private List<Vec3> split(ServerLevel level, LivingEntity owner, DiamondPrism prism, Vec3 look) {
        Vec3 c = prism.center();
        List<LivingEntity> foes = SpellUtil.enemiesAround(level, c, SPLIT_RANGE, owner, e -> level.clip(new ClipContext(c, e.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, prism)).getType() == HitResult.Type.MISS);
        foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c)));
        List<Vec3> out = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_SPLITS, foes.size()); i++) {
            out.add(foes.get(i).getBoundingBox().getCenter());
        }
        float[] fan = {-28, 28, 0};
        for (int i = out.size(); i < MAX_SPLITS; i++) {
            Vec3 dir = look.yRot(fan[i] * net.minecraft.util.Mth.DEG_TO_RAD);
            Vec3 to = c.add(dir.scale(SPLIT_RANGE));
            BlockHitResult block = level.clip(new ClipContext(c, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, prism));
            out.add(block.getType() == HitResult.Type.MISS ? to : block.getLocation());
        }
        return out;
    }

    /** Burns everything living along the beam from {@code a} to {@code b}. */
    private void burn(ServerLevel level, LivingEntity owner, AbstractSpell spell, Vec3 a, Vec3 b, float amount) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(1))) {
            if (e.isAlive() && !SpellUtil.isAlly(owner, e) && e.getBoundingBox().inflate(.35).clip(a, b).isPresent()) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, this, owner));
                e.igniteForSeconds(3);
                Vec3 at = e.getBoundingBox().getCenter();
                SpellUtil.particles(level, Gem.RUBY.glint, at.x, at.y, at.z, 4, .2, .3, .2, .05);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
