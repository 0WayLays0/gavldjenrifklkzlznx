package com.waylays.eraphones.gear;

import com.waylays.eraphones.event.EventRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The photograph a pinned photo holds (the very item, its picture, when and by whom), how askew it is, its pin's colour. */
public class PinnedPhotoBlockEntity extends BlockEntity {
    private ItemStack photo = ItemStack.EMPTY;
    private float angle;
    private int pin;

    public PinnedPhotoBlockEntity(BlockPos pos, BlockState state) {
        super(EventRegistry.PINNED_PHOTO_BE.get(), pos, state);
    }

    public ItemStack photo() {
        return photo;
    }

    /** Degrees turned about its face: a little askew on a wall; on a table, turned to face whoever laid it. */
    public float angle() {
        return angle;
    }

    /** The pin's colour (0 red, 1 yellow, 2 blue, 3 green, 4 white). */
    public int pin() {
        return pin;
    }

    public void set(ItemStack photo, float angle, int pin) {
        this.photo = photo.copyWithCount(1);
        this.angle = angle;
        this.pin = pin;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!photo.isEmpty()) {
            tag.put("Photo", photo.save(registries));
        }
        tag.putFloat("Angle", angle);
        tag.putInt("Pin", pin);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        photo = tag.contains("Photo") ? ItemStack.of(tag.getCompound("Photo")) : ItemStack.EMPTY;
        angle = tag.getFloat("Angle");
        pin = tag.getInt("Pin");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
