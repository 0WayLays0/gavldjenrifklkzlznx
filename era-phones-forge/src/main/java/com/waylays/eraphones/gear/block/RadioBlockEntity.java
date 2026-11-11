package com.waylays.eraphones.gear.block;

import com.waylays.eraphones.gear.Stations;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A radio's switch, frequency and volume, kept and sent to the players near it. */
public class RadioBlockEntity extends BlockEntity {
    private boolean on;
    private int frequency = 1003, volume = 6;

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(GearRegistry.RADIO_BE.get(), pos, state);
    }

    public boolean on() {
        return on;
    }

    public int frequency() {
        return frequency;
    }

    public int volume() {
        return volume;
    }

    public void set(boolean on, int frequency, int volume) {
        this.on = on;
        this.frequency = Math.max(Stations.MIN, Math.min(Stations.MAX, frequency));
        this.volume = Math.max(0, Math.min(10, volume));
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            Stations.RADIOS.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        Stations.RADIOS.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        Stations.RADIOS.remove(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("On", on);
        tag.putInt("Frequency", frequency);
        tag.putInt("Volume", volume);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        on = tag.getBoolean("On");
        frequency = tag.contains("Frequency") ? tag.getInt("Frequency") : 1003;
        volume = tag.contains("Volume") ? tag.getInt("Volume") : 6;
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
