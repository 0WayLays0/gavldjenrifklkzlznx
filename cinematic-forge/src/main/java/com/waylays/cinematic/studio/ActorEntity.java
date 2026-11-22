package com.waylays.cinematic.studio;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * An actor: a statue in a player's shape that replays its take. It never moves by itself (no physics, no AI), cannot
 * be hurt or pushed, and on the client follows the take tick by tick so the playback is smooth for everyone.
 */
public class ActorEntity extends LivingEntity {
    private static final EntityDataAccessor<String> ACTOR = SynchedEntityData.defineId(ActorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(ActorEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SLIM = SynchedEntityData.defineId(ActorEntity.class, EntityDataSerializers.BOOLEAN);

    /** Set by each side at start: the server's playback, the client's mirror of it. */
    public static FrameSource serverSource, clientSource;

    private final NonNullList<ItemStack> hands = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);

    private int lastTick = Integer.MIN_VALUE;
    private Track lastTrack;
    private final int[] lastEquip = {-1, -1, -1, -1, -1, -1};
    // What the take says, read by the renderer
    private boolean driven, flying, using, useOffhand;
    private int useTicks, flyTicks, emote, emoteTicks, prevEmote;
    private float swim, swimO;

    public ActorEntity(EntityType<? extends ActorEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ACTOR, "");
        entityData.define(SKIN, "");
        entityData.define(SLIM, false);
    }

    public UUID actorId() {
        try {
            String s = entityData.get(ACTOR);
            return s.isEmpty() ? null : UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setActorId(UUID id) {
        entityData.set(ACTOR, id.toString());
    }

    public String skin() {
        return entityData.get(SKIN);
    }

    public void setSkin(String hash) {
        entityData.set(SKIN, hash == null ? "" : hash);
    }

    public boolean slim() {
        return entityData.get(SLIM);
    }

    public void setSlim(boolean slim) {
        entityData.set(SLIM, slim);
    }

    // ------------------------------------------------------------------ playback

    @Override
    public void tick() {
        super.tick();
        drive();
    }

    @Override
    public void aiStep() {
        // A statue: the take moves it, nothing else does
    }

    private void drive() {
        FrameSource src = level().isClientSide ? clientSource : serverSource;
        UUID id = actorId();
        Track track = src == null || id == null ? null : src.track(id);
        if (track == null || track.isEmpty()) {
            driven = false;
            return;
        }
        int tick = Mth.clamp(src.tick(id), 0, track.length() - 1);
        boolean snap = !driven || track != lastTrack || Math.abs(tick - lastTick) > 2;
        driven = true;
        lastTrack = track;
        lastTick = tick;
        Frame f = track.frame(tick);

        double ox = getX(), oz = getZ();
        setPos(f.x, f.y, f.z);
        setYRot(f.yaw);
        setXRot(f.pitch);
        yHeadRot = f.headYaw;
        yBodyRot = f.bodyYaw;
        setPose(f.mcPose());
        setSprinting(f.has(Frame.SPRINT));
        setOnGround(f.has(Frame.GROUND));
        if (snap) {
            setOldPosAndRot();
            yBodyRotO = yBodyRot;
            yHeadRotO = yHeadRot;
            ox = getX();
            oz = getZ();
        }

        EquipmentSlot[] slots = EquipmentSlot.values();
        for (int s = 0; s < Frame.SLOTS && s < slots.length; s++) {
            if (f.equip[s] != lastEquip[s] || snap) {
                lastEquip[s] = f.equip[s];
                setItemSlot(slots[s], track.item(f.equip[s]).copy());
            }
        }

        // Body language read by the renderer
        flying = f.has(Frame.FLYING);
        flyTicks = flying ? flyTicks + 1 : 0;
        using = f.has(Frame.USING);
        useOffhand = f.has(Frame.USE_OFFHAND);
        useTicks = f.useTicks;
        emote = f.emote;
        emoteTicks = 0;
        while (emoteTicks < 10 && tick - emoteTicks - 1 >= 0 && track.frame(tick - emoteTicks - 1).emote == emote) emoteTicks++;
        prevEmote = emoteTicks < 10 && tick - emoteTicks - 1 >= 0 ? track.frame(tick - emoteTicks - 1).emote : 0;
        oAttackAnim = snap ? f.attack / 255f : attackAnim;
        attackAnim = f.attack / 255f;
        swingingArm = f.has(Frame.SWING_OFFHAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        swinging = f.attack > 0;
        swimO = swim;
        swim = getPose() == net.minecraft.world.entity.Pose.SWIMMING ? Math.min(1f, swim + 0.09f) : Math.max(0f, swim - 0.09f);
        if (snap) swimO = swim;
        float moved = (float) Math.sqrt((getX() - ox) * (getX() - ox) + (getZ() - oz) * (getZ() - oz)) * 4f;
        walkAnimation.update(Math.min(moved, 1f), 0.4f);
    }

    public boolean driven() {
        return driven;
    }

    public int emote() {
        return emote;
    }

    /** Ticks since the current gesture started (counted up to 10). */
    public int emoteTicks() {
        return emoteTicks;
    }

    /** The gesture before the current one, to blend out of. */
    public int prevEmote() {
        return prevEmote;
    }

    @Override
    public boolean isFallFlying() {
        return driven ? flying : super.isFallFlying();
    }

    @Override
    public int getFallFlyingTicks() {
        return driven ? flyTicks : super.getFallFlyingTicks();
    }

    @Override
    public boolean isUsingItem() {
        return driven ? using : super.isUsingItem();
    }

    @Override
    public InteractionHand getUsedItemHand() {
        return driven ? (useOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND) : super.getUsedItemHand();
    }

    @Override
    public ItemStack getUseItem() {
        return driven && using ? getItemInHand(getUsedItemHand()) : super.getUseItem();
    }

    @Override
    public int getTicksUsingItem() {
        return driven && using ? useTicks : super.getTicksUsingItem();
    }

    @Override
    public float getSwimAmount(float partialTick) {
        return driven ? Mth.lerp(partialTick, swimO, swim) : super.getSwimAmount(partialTick);
    }

    // Client: positions come from the take, not from the server's movement packets. Without a take the statue
    // jumps to where the server puts it (its movement step never runs).
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        if (driven) return;
        setPos(x, y, z);
        setRot(yRot, xRot);
        setOldPosAndRot();
    }

    @Override
    public void lerpHeadTo(float yaw, int steps) {
        if (!driven) {
            yHeadRot = yaw;
            yBodyRot = yaw;
        }
    }

    // ------------------------------------------------------------------ a statue

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    // ------------------------------------------------------------------ equipment

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return armor;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return switch (slot.getType()) {
            case HAND -> hands.get(slot.getIndex());
            case ARMOR -> armor.get(slot.getIndex());
        };
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        switch (slot.getType()) {
            case HAND -> hands.set(slot.getIndex(), stack);
            case ARMOR -> armor.set(slot.getIndex(), stack);
        }
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    // ------------------------------------------------------------------ saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Actor", entityData.get(ACTOR));
        tag.putString("Skin", skin());
        tag.putBoolean("Slim", slim());
        ListTag items = new ListTag();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            items.add(getItemBySlot(slot).save(new CompoundTag()));
        }
        tag.put("Gear", items);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ACTOR, tag.getString("Actor"));
        setSkin(tag.getString("Skin"));
        setSlim(tag.getBoolean("Slim"));
        ListTag items = tag.getList("Gear", 10);
        EquipmentSlot[] slots = EquipmentSlot.values();
        for (int i = 0; i < items.size() && i < slots.length; i++) {
            setItemSlot(slots[i], ItemStack.of(items.getCompound(i)));
        }
    }
}
