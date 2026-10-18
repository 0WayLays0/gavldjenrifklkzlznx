package com.waylays.arcanes.entity.explosion;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.explosion.DetonationWaveSpell;
import com.waylays.arcanes.spells.explosion.ExplosionFx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Invisible conductor of Detonation Wave: every {@link #RING_TICKS} ticks a ring of blasts goes off a little further
 * from the caster, like a wave of fire rolling outward. Each enemy is hit once, by the first ring that reaches it.
 */
public class DetonationWave extends Entity {
    public static final int RING_TICKS = 3;
    /** Half the width of the band of ground a ring hurts. */
    public static final double BAND = 1.3;

    @Nullable
    private UUID ownerUUID;
    private float damage;
    private int rings;
    private int ring;
    private final Set<UUID> hit = new HashSet<>();

    public DetonationWave(EntityType<? extends DetonationWave> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    public DetonationWave(Level level, LivingEntity owner, float damage, int rings) {
        this(ModEntities.DETONATION_WAVE.get(), level);
        this.ownerUUID = owner.getUUID();
        this.damage = damage;
        this.rings = rings;
        hit.add(owner.getUUID());
    }

    public static double ringRadius(int ring) {
        return 2 + ring * DetonationWaveSpell.RING_SPACING;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount % RING_TICKS != 0) {
            return;
        }
        if (ring >= rings) {
            discard();
            return;
        }
        Entity owner = ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
        Vec3 center = position();
        double radius = ringRadius(ring);
        // Blasts about 2.2 blocks apart along the ring, each one sitting on the ground
        int count = Math.max(6, (int) (Math.PI * 2 * radius / 2.2));
        double offset = random.nextDouble() * Math.PI * 2;
        float size = .75f + ring * .06f;
        for (int i = 0; i < count; i++) {
            double a = offset + i * Math.PI * 2 / count;
            Vec3 at = SpellUtil.groundAt(level(), center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius), 3, 1);
            ExplosionFx.lightBlast(level(), at.add(0, .2, 0), size, ring % 2 == 0 ? BlastEffect.STYLE_FIRE : BlastEffect.STYLE_ARCANE);
        }
        for (LivingEntity e : SpellUtil.enemiesAround(level(), center, radius + BAND, owner, x -> !hit.contains(x.getUUID()))) {
            double dx = e.getX() - center.x;
            double dz = e.getZ() - center.z;
            double flat = Math.sqrt(dx * dx + dz * dz);
            if ((ring > 0 && flat < radius - BAND) || Math.abs(e.getY() - center.y) > 4) {
                continue;
            }
            hit.add(e.getUUID());
            if (DamageSources.applyDamage(e, damage, SpellUtil.damageSource(ModSpells.DETONATION_WAVE.get(), this, owner))) {
                double push = flat < 1.0E-3 ? 0 : .7 / flat;
                e.push(dx * push, .45, dz * push);
                e.hurtMarked = true;
            }
        }
        float pitch = .9f + ring * .08f;
        level().playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 2f, pitch);
        level().playSound(null, center.x, center.y, center.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 1.5f, pitch * .7f);
        SpellUtil.cameraShake(level(), center, 4, (float) radius + 8);
        ring++;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
