package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gem.Gem;
import com.waylays.arcanes.spells.gem.GemFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * The Diamond Prism: a great cut diamond hovering over the spot aimed at, turning slowly, a ring of shards around it.
 * Every second it refracts the light into rainbow rays that strike up to three enemies around it. A Ruby Ray of its
 * caster's that meets it is caught and split in three (see {@link RubyRay}); while it holds a ray it flares.
 */
public class DiamondPrism extends SpellEffectEntity {
    /** How close to its heart a beam must pass to be caught. */
    public static final float CATCH = 1.1f;
    public static final float HOVER = 1.7f;
    public static final int FIRE_TICKS = 20;
    public static final float RANGE = 10;
    public static final int MAX_SHOTS = 3;
    /** How many prisms one caster may keep: a new one takes the oldest one's place. */
    public static final int MAX_PER_CASTER = 2;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(DiamondPrism.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHOT_AT = SynchedEntityData.defineId(DiamondPrism.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CAUGHT = SynchedEntityData.defineId(DiamondPrism.class, EntityDataSerializers.INT);
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Integer>[] DATA_SHOT = new EntityDataAccessor[MAX_SHOTS];

    static {
        for (int i = 0; i < MAX_SHOTS; i++) {
            DATA_SHOT[i] = SynchedEntityData.defineId(DiamondPrism.class, EntityDataSerializers.INT);
        }
    }

    @Nullable
    private String spellId;

    public DiamondPrism(EntityType<? extends DiamondPrism> entityType, Level level) {
        super(entityType, level);
    }

    /** A prism hovering over {@code ground}; the caster's oldest prism breaks if they already keep the most they may. */
    public static DiamondPrism place(Level level, LivingEntity caster, AbstractSpell spell, Vec3 ground, int life, float damage) {
        List<DiamondPrism> mine = level.getEntitiesOfClass(DiamondPrism.class, caster.getBoundingBox().inflate(96),
                p -> !p.isRemoved() && caster.getUUID().equals(p.getOwnerUUID()));
        mine.sort(Comparator.comparingInt(p -> -p.tickCount));
        for (int i = 0; mine.size() - i >= MAX_PER_CASTER; i++) {
            mine.get(i).shatter();
        }
        DiamondPrism prism = new DiamondPrism(ModEntities.DIAMOND_PRISM.get(), level);
        prism.setOwner(caster);
        prism.spellId = spell.getSpellId();
        prism.damage = damage;
        prism.moveTo(ground.x, ground.y + HOVER, ground.z, level.random.nextFloat() * 360, 0);
        prism.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(prism);
        GemFx.chime(level, prism.center(), 1.5f, 1.4f);
        GemFx.glitter(level, prism.center(), Gem.DIAMOND, 30, .8);
        return prism;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 300);
        builder.define(DATA_SHOT_AT, -100);
        builder.define(DATA_CAUGHT, -100);
        for (EntityDataAccessor<Integer> shot : DATA_SHOT) {
            builder.define(shot, -1);
        }
    }

    public Vec3 center() {
        return position();
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** The tick it last fired its rays, and at whom (entity ids, -1 for none). */
    public int getShotAt() {
        return this.entityData.get(DATA_SHOT_AT);
    }

    public int getShot(int i) {
        return this.entityData.get(DATA_SHOT[i]);
    }

    /** The tick a Ruby Ray last went through it. */
    public int getCaught() {
        return this.entityData.get(DATA_CAUGHT);
    }

    void catchRay() {
        this.entityData.set(DATA_CAUGHT, tickCount);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount >= getLife()) {
            shatter();
            return;
        }
        if (tickCount > 10 && tickCount % FIRE_TICKS == 0) {
            fire(level);
        }
    }

    /** Rainbow rays at up to three enemies in sight. */
    private void fire(ServerLevel level) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity owner = getLivingOwner();
        Vec3 c = center();
        List<LivingEntity> foes = SpellUtil.enemiesAround(level, c, RANGE, owner, e -> level.clip(new ClipContext(c, e.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS);
        foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c)));
        for (int i = 0; i < MAX_SHOTS; i++) {
            LivingEntity foe = i < foes.size() ? foes.get(i) : null;
            this.entityData.set(DATA_SHOT[i], foe == null ? -1 : foe.getId());
            if (foe != null && spell != null) {
                foe.invulnerableTime = 0;
                DamageSources.applyDamage(foe, damage, SpellUtil.damageSource(spell, this, owner));
                GemFx.glitter(level, foe.getBoundingBox().getCenter(), Gem.DIAMOND, 8, .3);
            }
        }
        if (!foes.isEmpty()) {
            this.entityData.set(DATA_SHOT_AT, tickCount);
            GemFx.chime(level, c, 1f, 1.6f + random.nextFloat() * .3f);
        }
    }

    public void shatter() {
        if (!isRemoved()) {
            GemFx.shatter(level(), center(), Gem.DIAMOND, 1.6f, 18);
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
