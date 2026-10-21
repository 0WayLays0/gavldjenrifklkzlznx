package com.waylays.arcanes.entity.glyph;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import com.waylays.arcanes.spells.glyph.GlyphFx;
import com.waylays.arcanes.spells.glyph.GlyphReactions;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A bolt of written element: a flame comet, an ice needle, a crackling spark, a water orb, a ball of wind or a
 * boulder, its glyph turning behind it. Its hit inscribes its element on the victim (or sets off a reaction); a
 * splashing bolt does so to everyone around where it lands.
 */
public class GlyphBolt extends SpellProjectile {
    private static final EntityDataAccessor<Integer> DATA_ELEMENT = SynchedEntityData.defineId(GlyphBolt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(GlyphBolt.class, EntityDataSerializers.FLOAT);

    private final IntOpenHashSet struck = new IntOpenHashSet();
    @Nullable
    private String spellId;
    private float splash;
    private double fall;
    private int pierce;

    public GlyphBolt(EntityType<? extends GlyphBolt> entityType, Level level) {
        super(entityType, level);
    }

    /** Fires a bolt from {@code from} along {@code direction}. */
    public static GlyphBolt shoot(Level level, @Nullable LivingEntity owner, AbstractSpell spell, GlyphElement element, Vec3 from, Vec3 direction, float speed, float damage) {
        GlyphBolt bolt = new GlyphBolt(ModEntities.GLYPH_BOLT.get(), level);
        bolt.setOwner(owner);
        bolt.spellId = spell.getSpellId();
        bolt.entityData.set(DATA_ELEMENT, element.ordinal());
        bolt.setDamage(damage);
        bolt.setPos(from.x, from.y - bolt.getBbHeight() * .5, from.z);
        bolt.shoot(direction, speed);
        level.addFreshEntity(bolt);
        return bolt;
    }

    public GlyphBolt withSplash(float radius) {
        this.splash = radius;
        return this;
    }

    public GlyphBolt withGravity(double gravity) {
        this.fall = gravity;
        return this;
    }

    public GlyphBolt withSize(float size) {
        this.entityData.set(DATA_SIZE, size);
        return this;
    }

    public GlyphBolt withPierce(int count) {
        this.pierce = count;
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ELEMENT, 0);
        builder.define(DATA_SIZE, 1f);
    }

    public GlyphElement getElement() {
        return GlyphElement.byId(this.entityData.get(DATA_ELEMENT));
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    @Override
    protected double gravity() {
        return fall;
    }

    @Override
    protected int lifetime() {
        return 60;
    }

    @Override
    protected boolean piercesEntities() {
        return pierce > 0;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !struck.contains(target.getId()) && !SpellUtil.isAlly(getOwner(), target)
                && !(target instanceof GlyphBolt);
    }

    @Override
    protected void onImpact(HitResult hit) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null || !(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        Vec3 at = hit.getLocation();
        Entity owner = getOwner();
        if (hit instanceof EntityHitResult entityHit) {
            struck.add(entityHit.getEntity().getId());
            if (splash <= 0 && entityHit.getEntity() instanceof LivingEntity target) {
                GlyphReactions.strike(level, owner, this, target, getElement(), damage, spell);
            }
            if (splash <= 0 && pierce-- > 0) {
                GlyphFx.burst(level, getElement(), at, 6, .2);
                return;
            }
        }
        if (splash > 0) {
            for (LivingEntity target : SpellUtil.enemiesAround(level, at, splash, owner, x -> true)) {
                GlyphReactions.strike(level, owner, this, target, getElement(), damage, spell);
            }
        }
        GlyphFx.impact(level, getElement(), at, Math.max(splash, .6f) * getSize());
        discard();
    }

    @Override
    protected void clientTick() {
        GlyphFx.trail(level(), getElement(), position().add(0, getBbHeight() * .5, 0), getSize(), random);
    }
}
