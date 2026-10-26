package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.entity.OwnedSummon;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.SummoningSpell;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import com.waylays.arcanes.spells.cosmos.CosmosFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A variant of the caster from another branch of time: the caster's own likeness in flickering orange light, scanned
 * in beside them. It looks where they look and casts one of the other spells of their spellbook at the level it holds
 * there, then glitches out of existence. It cannot be hurt, and its spells spare its original and their allies.
 */
public class CosmicVariant extends Mob implements OwnedSummon, AntiMagicSusceptible {
    public static final int CAST_TICK = 12;
    public static final int LIFE = 46;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(CosmicVariant.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_CAST = SynchedEntityData.defineId(CosmicVariant.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private int spellLevel = 1;
    private Vec3 aim = Vec3.ZERO;

    public CosmicVariant(EntityType<? extends CosmicVariant> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        setInvulnerable(true);
        setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0);
    }

    /** Scans in a variant of {@code caster} at {@code at}; it will cast a spell of theirs at what they are looking at. */
    @Nullable
    public static CosmicVariant scanIn(Level level, LivingEntity caster, Vec3 at) {
        SpellSlot slot = pick(caster);
        if (slot == null) {
            return null;
        }
        CosmicVariant variant = new CosmicVariant(ModEntities.COSMIC_VARIANT.get(), level);
        variant.entityData.set(DATA_OWNER, Optional.of(caster.getUUID()));
        variant.spellId = slot.getSpell().getSpellId();
        variant.spellLevel = slot.getLevel();
        // It looks at what its original looks at
        Vec3 eye = caster.getEyePosition();
        HitResult hit = level.clip(new ClipContext(eye, eye.add(caster.getLookAngle().scale(32)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        variant.aim = hit.getLocation();
        Vec3 d = variant.aim.subtract(at.add(0, caster.getEyeHeight(), 0));
        float yRot = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
        float xRot = (float) (-Mth.atan2(d.y, d.horizontalDistance()) * Mth.RAD_TO_DEG);
        variant.moveTo(at.x, at.y, at.z, yRot, xRot);
        variant.setYHeadRot(yRot);
        variant.setYBodyRot(yRot);
        level.addFreshEntity(variant);
        CosmosFx.time(level, at.add(0, 1, 0), 30, .5);
        CosmosFx.sound(level, at, SoundEvents.BEACON_POWER_SELECT, 1, 1.7f);
        CosmosFx.sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 1, .6f);
        return variant;
    }

    /** A spell of the caster's spellbook (any but this one and the few a copy cannot cast); for a creature, the baton. */
    @Nullable
    private static SpellSlot pick(LivingEntity caster) {
        List<SpellSlot> slots = new ArrayList<>();
        if (caster instanceof Player player) {
            ItemStack book = Utils.getPlayerSpellbookStack(player);
            if (book != null && ISpellContainer.isSpellContainer(book)) {
                for (SpellSlot slot : ISpellContainer.get(book).getActiveSpells()) {
                    if (castable(slot.getSpell())) {
                        slots.add(slot);
                    }
                }
            }
        }
        if (slots.isEmpty()) {
            AbstractSpell baton = ModSpells.PRUNING_BATON.get();
            return SpellSlot.of(new io.redspace.ironsspellbooks.api.spells.SpellData(baton, 3), 0);
        }
        return slots.get(caster.getRandom().nextInt(slots.size()));
    }

    public static boolean castable(@Nullable AbstractSpell spell) {
        if (spell == null || spell == SpellRegistry.none() || !spell.isEnabled() || spell.getCastType() == CastType.CONTINUOUS
                || spell instanceof SummoningSpell) {
            return false;
        }
        String id = spell.getSpellId();
        return !id.equals(ModSpells.VARIANT.get().getSpellId()) && !id.equals(ModSpells.POCKET_UNIVERSE.get().getSpellId()) && !id.contains("summon");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_CAST, -1);
    }

    /** The tick it cast its spell (-1 not yet): the client raises its arm then. */
    public int getCastTick() {
        return this.entityData.get(DATA_CAST);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(2) == 0) {
                level().addParticle(CosmosFx.TIME, getRandomX(.6), getY() + random.nextDouble() * getBbHeight(), getRandomZ(.6), 0, .01, 0);
            }
            return;
        }
        if (tickCount == CAST_TICK) {
            cast();
        }
        if (tickCount >= LIFE) {
            glitchOut();
        }
    }

    private void cast() {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null || !(level() instanceof ServerLevel level)) {
            return;
        }
        this.entityData.set(DATA_CAST, tickCount);
        MagicData data = new MagicData(true);
        try {
            spell.checkPreCastConditions(level, spellLevel, this, data);
            spell.onCast(level, spellLevel, this, CastSource.MOB, data);
        } catch (RuntimeException e) {
            // A spell that cannot be cast by a copy fizzles out
            CosmosFx.time(level, getEyePosition(), 10, .3);
        }
        CosmosFx.sound(level, position(), SoundEvents.EVOKER_CAST_SPELL, .8f, 1.3f);
    }

    private void glitchOut() {
        CosmosFx.time(level(), position().add(0, 1, 0), 40, .45);
        SpellUtil.particles(level(), CosmosFx.PRUNED, getX(), getY() + 1, getZ(), 30, .3, .6, .3, .02);
        CosmosFx.sound(level(), position(), SoundEvents.BEACON_DEACTIVATE, .8f, 1.8f);
        discard();
    }

    @Override
    @Nullable
    public UUID getSummonerUUID() {
        return this.entityData.get(DATA_OWNER).orElse(null);
    }

    @Override
    @Nullable
    public LivingEntity getSummoner() {
        UUID uuid = getSummonerUUID();
        return uuid != null && level() instanceof ServerLevel level && level.getEntity(uuid) instanceof LivingEntity living ? living : null;
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.VARIANT.get();
    }

    @Override
    public void dismissSummon() {
        glitchOut();
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        glitchOut();
    }

    /** Its original, and its original's allies, are its allies. */
    @Override
    public boolean isAlliedTo(Entity other) {
        LivingEntity owner = getSummoner();
        return super.isAlliedTo(other) || owner != null && (other == owner || Cosmos.isAlly(owner, other));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
