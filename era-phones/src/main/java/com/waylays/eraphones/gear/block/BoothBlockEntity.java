package com.waylays.eraphones.gear.block;

import com.waylays.eraphones.registry.GearRegistry;
import com.waylays.eraphones.server.Booths;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A phone booth's number, kept with it; the booth known to the phone network while its chunk is loaded. */
public class BoothBlockEntity extends BlockEntity {
    private String number = "";

    public BoothBlockEntity(BlockPos pos, BlockState state) {
        super(GearRegistry.BOOTH_BE.get(), pos, state);
    }

    public String number() {
        return number;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel sl) {
            if (number.isEmpty()) {
                number = Booths.newNumber(sl.getServer());
                setChanged();
            }
            Booths.put(number, GlobalPos.of(sl.dimension(), worldPosition));
        }
    }

    private void forget() {
        if (level != null && !level.isClientSide && !number.isEmpty()) {
            Booths.remove(number, GlobalPos.of(level.dimension(), worldPosition));
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        forget();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        forget();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Number", number);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        number = tag.getString("Number");
    }
}
