package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

import javax.annotation.Nullable;

/**
 * Barkskin: plates of living bark grow up the caster's body from their feet, thorns sticking out of them. While it
 * holds they take far less damage and are hardly pushed back; whoever strikes them from close by is torn by the thorns
 * and thrown back; the life in the wood mends them a little now and then. When its time is up the bark cracks and
 * falls away.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class BarkShell extends SpellEffectEntity {
    public static final int GROW = 14;
    public static final int SHED = 12;
    /** Damage taken through the bark, and how much a blow's push is kept. */
    public static final float GUARD = .55f;
    public static final float KNOCKBACK = .2f;

    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(BarkShell.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(BarkShell.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private float mend;

    public BarkShell(EntityType<? extends BarkShell> entityType, Level level) {
        super(entityType, level);
    }

    /** Grows the bark on the caster (bark already on them is replaced). {@code thorns}: what a striker takes. */
    public static BarkShell grow(Level level, LivingEntity caster, AbstractSpell spell, int duration, float thorns, float mend) {
        BarkShell old = of(level, caster);
        if (old != null) {
            old.discard();
        }
        BarkShell shell = new BarkShell(ModEntities.BARK_SHELL.get(), level);
        shell.setOwner(caster);
        shell.spellId = spell.getSpellId();
        shell.damage = thorns;
        shell.mend = mend;
        shell.moveTo(caster.getX(), caster.getY(), caster.getZ(), caster.getYRot(), 0);
        shell.entityData.set(DATA_DURATION, duration);
        shell.entityData.set(DATA_OWNER, caster.getId());
        level.addFreshEntity(shell);
        WoodFx.creak(level, caster.position(), 1.2f);
        WoodFx.sprout(level, caster.position(), 1);
        WoodFx.chips(level, caster.position().add(0, .3, 0), 14, .4);
        return shell;
    }

    /** The bark on {@code caster}, if any. */
    @Nullable
    public static BarkShell of(Level level, LivingEntity caster) {
        for (BarkShell shell : level.getEntitiesOfClass(BarkShell.class, caster.getBoundingBox().inflate(4), s -> caster.getUUID().equals(s.getOwnerUUID()))) {
            if (!shell.isRemoved() && shell.tickCount <= shell.getDuration()) {
                return shell;
            }
        }
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DURATION, 400);
        builder.define(DATA_OWNER, -1);
    }

    public int getDuration() {
        return this.entityData.get(DATA_DURATION);
    }

    /** The one wearing the bark, on both sides. */
    @Nullable
    public Entity wearer() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    /** How far up the body the bark has grown (0 to 1), and back down as it sheds. */
    public float grown(float age) {
        float up = Mth.clamp(age / GROW, 0, 1);
        float down = Mth.clamp((age - getDuration()) / SHED, 0, 1);
        return (1 - (1 - up) * (1 - up)) * (1 - down);
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = wearer();
        if (owner != null) {
            setPos(owner.getX(), owner.getY(), owner.getZ());
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity caster = getLivingOwner();
        if (caster == null || !caster.isAlive()) {
            discard();
            return;
        }
        if (tickCount == getDuration()) {
            WoodFx.crash(level, position(), .8f);
            WoodFx.chips(level, position().add(0, 1, 0), 30, .5);
        }
        if (tickCount > getDuration() + SHED) {
            discard();
            return;
        }
        if (tickCount < getDuration() && tickCount % 40 == 20 && caster.getHealth() < caster.getMaxHealth()) {
            caster.heal(mend);
            WoodFx.life(level, position().add(0, 1, 0), 6, .4);
        }
    }

    /** Less damage through the bark; a blow from close by tears the striker on the thorns. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        BarkShell shell = of(level, victim);
        if (shell == null || shell.tickCount < GROW / 2) {
            return;
        }
        event.setAmount(event.getAmount() * GUARD);
        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof LivingEntity attacker && attacker != victim && attacker.distanceTo(victim) < 4.5f && !SpellUtil.isAlly(victim, attacker)) {
            AbstractSpell spell = shell.spellId == null ? null : SpellRegistry.getSpell(shell.spellId);
            if (spell != null) {
                attacker.invulnerableTime = 0;
                DamageSources.applyDamage(attacker, shell.damage, SpellUtil.damageSource(spell, shell, victim));
                double resist = 1 - Mth.clamp(attacker.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                Vec3 away = attacker.position().subtract(victim.position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                attacker.setDeltaMovement(attacker.getDeltaMovement().add(away.scale(.5 * resist)).add(0, .15 * resist, 0));
                attacker.hurtMarked = true;
            }
            Vec3 at = victim.position().add(0, 1, 0).add(attacker.position().subtract(victim.position()).normalize().scale(.5));
            WoodFx.chips(level, at, 8, .2);
            WoodFx.sound(level, at, SoundEvents.WOOD_HIT, 1.2f, .6f);
        }
    }

    /** Rooted: the bark hardly gives to a push. */
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level() instanceof ServerLevel level && of(level, victim) != null) {
            event.setStrength(event.getStrength() * KNOCKBACK);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
