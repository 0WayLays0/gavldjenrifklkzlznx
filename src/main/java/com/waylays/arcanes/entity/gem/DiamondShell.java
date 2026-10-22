package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gem.Gem;
import com.waylays.arcanes.spells.gem.GemFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Diamond Shell: twelve plates of cut diamond close round the caster in a turning shell. Each blow that reaches
 * them loses most of its strength on a plate, and the plate breaks off and flies back at whoever struck. What is left
 * of the shell when its time is up bursts out in shards at the enemies around.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class DiamondShell extends SpellEffectEntity {
    /** The twelve plates face the corners of an icosahedron. */
    public static final int PLATES = 12;
    public static final float RADIUS = 1.35f;
    /** The part of a blow a plate takes. */
    public static final float ABSORB = .6f;
    public static final float BURST_RANGE = 8;
    private static final Vec3[] FACING = new Vec3[PLATES];

    static {
        double phi = (1 + Math.sqrt(5)) / 2;
        double[][] v = {{-1, phi, 0}, {1, phi, 0}, {-1, -phi, 0}, {1, -phi, 0}, {0, -1, phi}, {0, 1, phi},
                {0, -1, -phi}, {0, 1, -phi}, {phi, 0, -1}, {phi, 0, 1}, {-phi, 0, -1}, {-phi, 0, 1}};
        for (int i = 0; i < PLATES; i++) {
            FACING[i] = new Vec3(v[i][0], v[i][1], v[i][2]).normalize();
        }
    }

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(DiamondShell.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PLATES = SynchedEntityData.defineId(DiamondShell.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(DiamondShell.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public DiamondShell(EntityType<? extends DiamondShell> entityType, Level level) {
        super(entityType, level);
    }

    /** The caster's shell (a new one replaces the old). */
    public static DiamondShell encase(Level level, LivingEntity caster, AbstractSpell spell, int life, float damage) {
        for (DiamondShell old : level.getEntitiesOfClass(DiamondShell.class, caster.getBoundingBox().inflate(6),
                s -> caster.getUUID().equals(s.getOwnerUUID()))) {
            old.discard();
        }
        DiamondShell shell = new DiamondShell(ModEntities.DIAMOND_SHELL.get(), level);
        shell.setOwner(caster);
        shell.spellId = spell.getSpellId();
        shell.damage = damage;
        shell.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        shell.entityData.set(DATA_OWNER, caster.getId());
        shell.entityData.set(DATA_PLATES, (1 << PLATES) - 1);
        shell.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(shell);
        GemFx.chime(level, caster.position(), 1.4f, 1.2f);
        GemFx.glitter(level, caster.position().add(0, 1, 0), Gem.DIAMOND, 30, 1.2);
        return shell;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_PLATES, (1 << PLATES) - 1);
        builder.define(DATA_LIFE, 300);
    }

    @Nullable
    public Entity getCaster() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    public int getPlates() {
        return this.entityData.get(DATA_PLATES);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** Which way plate {@code i} faces at age {@code age}: the shell turns slowly on itself. */
    public static Vec3 facing(int i, float age) {
        return FACING[i].yRot(age * .03f).xRot(.25f * Mth.sin(age * .02f));
    }

    /** Where the shell's middle is, from the caster's feet. */
    public static double middle(Entity caster) {
        return caster.getBbHeight() * .5;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity owner = getLivingOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        setPos(owner.getX(), owner.getY(), owner.getZ());
        if (tickCount >= getLife() || getPlates() == 0) {
            burst(level, owner);
        }
    }

    /** A blow on the shell: a plate takes most of it, breaks off and flies back at whoever struck. */
    private float takeBlow(ServerLevel level, LivingEntity owner, float amount, @Nullable Entity attacker) {
        int plates = getPlates();
        if (plates == 0) {
            return amount;
        }
        Vec3 c = position().add(0, middle(owner), 0);
        Vec3 from = attacker != null ? attacker.getBoundingBox().getCenter().subtract(c) : new Vec3(0, 1, 0);
        int best = -1;
        double bestDot = -2;
        for (int i = 0; i < PLATES; i++) {
            if ((plates & (1 << i)) != 0) {
                double dot = facing(i, tickCount).dot(from.normalize());
                if (dot > bestDot) {
                    bestDot = dot;
                    best = i;
                }
            }
        }
        this.entityData.set(DATA_PLATES, plates & ~(1 << best));
        Vec3 plate = c.add(facing(best, tickCount).scale(RADIUS));
        GemFx.shatter(level, plate, Gem.DIAMOND, .6f, 5);
        GemFx.sound(level, plate, SoundEvents.AMETHYST_BLOCK_BREAK, 1.2f, 1.5f);
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (attacker instanceof LivingEntity striker && striker.isAlive() && !SpellUtil.isAlly(owner, striker)
                && striker.distanceToSqr(owner) < 24 * 24 && spell != null) {
            GemShard.fling(level, owner, spell, Gem.DIAMOND, plate, striker, damage);
        }
        return amount * (1 - ABSORB);
    }

    /** Its time is up: what is left bursts out in shards at the enemies around. */
    private void burst(ServerLevel level, LivingEntity owner) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        Vec3 c = position().add(0, middle(owner), 0);
        List<LivingEntity> foes = SpellUtil.enemiesAround(level, c, BURST_RANGE, owner, owner::hasLineOfSight);
        int plates = getPlates();
        int k = 0;
        for (int i = 0; i < PLATES; i++) {
            if ((plates & (1 << i)) == 0) {
                continue;
            }
            Vec3 plate = c.add(facing(i, tickCount).scale(RADIUS));
            if (!foes.isEmpty() && spell != null) {
                GemShard.fling(level, owner, spell, Gem.DIAMOND, plate, foes.get(k++ % foes.size()), damage * .6f);
            } else {
                GemFx.glitter(level, plate, Gem.DIAMOND, 4, .2);
            }
        }
        GemFx.shatter(level, c, Gem.DIAMOND, 1.4f, 12);
        discard();
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || event.getNewDamage() <= 0) {
            return;
        }
        for (DiamondShell shell : level.getEntitiesOfClass(DiamondShell.class, victim.getBoundingBox().inflate(3),
                s -> !s.isRemoved() && victim.getUUID().equals(s.getOwnerUUID()) && s.getPlates() != 0)) {
            event.setNewDamage(shell.takeBlow(level, victim, event.getNewDamage(), event.getSource().getEntity()));
            return;
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
