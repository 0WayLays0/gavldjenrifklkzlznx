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
 * A variant of the caster from another branch of time: the caster's own likeness in flickering light, scanned in
 * beside them, a TVA designation hanging over its head. As it arrives it casts one of the other spells of their
 * spellbook at what they are looking at; then, as long as it lasts, every spell its original casts, it casts too, a
 * beat later, at the same thing. It cannot be hurt, and its spells spare its original and their allies. When its time
 * is up it glitches out of existence.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = com.waylays.arcanes.ArcanesOublies.MODID)
public class CosmicVariant extends Mob implements OwnedSummon, AntiMagicSusceptible {
    public static final int CAST_TICK = 12;
    /** Ticks it takes to scan in, and to break up. */
    public static final int SCAN_TICKS = 10;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(CosmicVariant.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_CAST = SynchedEntityData.defineId(CosmicVariant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(CosmicVariant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DESIGNATION = SynchedEntityData.defineId(CosmicVariant.class, EntityDataSerializers.INT);

    /** A spell its original cast, to be cast again a beat later. */
    private record Echo(String spellId, int level, Vec3 aim, int at) {}

    private final java.util.List<Echo> echoes = new java.util.ArrayList<>();

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
    /** Scans in a variant of {@code caster} at {@code at} for {@code life} ticks. */
    @Nullable
    public static CosmicVariant scanIn(Level level, LivingEntity caster, Vec3 at) {
        return scanIn(level, caster, at, 46);
    }

    @Nullable
    public static CosmicVariant scanIn(Level level, LivingEntity caster, Vec3 at, int life) {
        SpellSlot slot = pick(caster);
        if (slot == null) {
            return null;
        }
        CosmicVariant variant = new CosmicVariant(ModEntities.COSMIC_VARIANT.get(), level);
        variant.entityData.set(DATA_OWNER, Optional.of(caster.getUUID()));
        variant.entityData.set(DATA_LIFE, life);
        variant.entityData.set(DATA_DESIGNATION, 1000 + caster.getRandom().nextInt(9000));
        variant.spellId = slot.getSpell().getSpellId();
        variant.spellLevel = slot.getLevel();
        variant.moveTo(at.x, at.y, at.z, caster.getYRot(), caster.getXRot());
        variant.faceAim(aimOf(caster));
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

    /** What {@code caster} is looking at: the first block in their line of sight, at most 32 blocks away. */
    private static Vec3 aimOf(LivingEntity caster) {
        Vec3 eye = caster.getEyePosition();
        HitResult hit = caster.level().clip(new ClipContext(eye, eye.add(caster.getLookAngle().scale(32)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        return hit.getLocation();
    }

    /** Turns to look at {@code target} from its eyes. */
    private void faceAim(Vec3 target) {
        this.aim = target;
        Vec3 d = target.subtract(getEyePosition());
        float yRot = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
        float xRot = (float) (-Mth.atan2(d.y, d.horizontalDistance()) * Mth.RAD_TO_DEG);
        setYRot(yRot);
        setXRot(xRot);
        yRotO = yRot;
        xRotO = xRot;
        setYHeadRot(yRot);
        setYBodyRot(yRot);
    }

    /** Every spell a player casts, their variants cast too, a beat later each, at what the player is looking at. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onOriginalCasts(io.redspace.ironsspellbooks.api.events.SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        if (!castable(spell)) {
            return;
        }
        Vec3 aim = aimOf(player);
        int k = 0;
        for (CosmicVariant variant : player.level().getEntitiesOfClass(CosmicVariant.class, player.getBoundingBox().inflate(32),
                v -> player.getUUID().equals(v.getSummonerUUID()) && v.tickCount < v.getLife() - SCAN_TICKS)) {
            variant.echoes.add(new Echo(spell.getSpellId(), event.getSpellLevel(), aim, variant.tickCount + 4 + 3 * k++));
        }
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** Its TVA designation (L-xxxx), shown over its head. */
    public int getDesignation() {
        return this.entityData.get(DATA_DESIGNATION);
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
        builder.define(DATA_LIFE, 46);
        builder.define(DATA_DESIGNATION, 1130);
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
            cast(spellId, spellLevel);
        }
        for (Echo echo : java.util.List.copyOf(echoes)) {
            if (tickCount >= echo.at()) {
                echoes.remove(echo);
                faceAim(echo.aim());
                cast(echo.spellId(), echo.level());
            }
        }
        if (tickCount >= getLife()) {
            glitchOut();
        }
    }

    private void cast(@Nullable String id, int level0) {
        AbstractSpell spell = id == null ? null : SpellRegistry.getSpell(id);
        if (spell == null || !(level() instanceof ServerLevel level)) {
            return;
        }
        int spellLevel = Math.max(1, level0);
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
