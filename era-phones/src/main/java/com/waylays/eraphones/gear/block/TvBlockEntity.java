package com.waylays.eraphones.gear.block;

import com.waylays.eraphones.gear.Audio;
import com.waylays.eraphones.gear.Tape;
import com.waylays.eraphones.gear.TvNet;
import com.waylays.eraphones.gear.Video;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A television, and the video player built into it: a cassette pushed in plays at once on its own screen (its sound
 * from the set), switching the set off pauses it, on again goes on. Without a cassette of its own, it shows what a
 * video recorder beside it plays.
 */
public class TvBlockEntity extends BlockEntity {
    private ItemStack cassette = ItemStack.EMPTY;
    private final Tape.Deck deck = new Tape.Deck();
    private Object sound;
    private int shownFrame = Integer.MIN_VALUE;
    private long shownAt;

    public TvBlockEntity(BlockPos pos, BlockState state) {
        super(GearRegistry.TV_BE.get(), pos, state);
    }

    public boolean on() {
        return getBlockState().getValue(TvBlock.LIT);
    }

    public boolean hasCassette() {
        return !cassette.isEmpty();
    }

    /** Takes {@code tape} (one cassette) and plays it, the set switched on. */
    public void insert(ItemStack tape) {
        cassette = tape;
        deck.file = tape.get(GearRegistry.TAPE.get());
        deck.pos = tape.getOrDefault(GearRegistry.TAPE_POS.get(), 0L);
        if (deck.file != null) {
            Tape.sound(deck.file);
            if (Video.isVideo(deck.file)) {
                Video.prepare(deck.file);
            }
            deck.mode = Tape.Mode.PLAY;
        }
        shownFrame = Integer.MIN_VALUE;
        setChanged();
    }

    /** Takes the cassette out, the tape left where it was. */
    public ItemStack takeCassette() {
        stopSound();
        ItemStack out = cassette;
        if (!out.isEmpty()) {
            out.set(GearRegistry.TAPE_POS.get(), deck.pos);
        }
        cassette = ItemStack.EMPTY;
        deck.file = null;
        deck.mode = Tape.Mode.STOP;
        deck.pos = 0;
        shownFrame = Integer.MIN_VALUE;
        setChanged();
        return out;
    }

    /** The set switched on or off: the tape goes on or waits. */
    public void power(boolean on) {
        if (cassette.isEmpty() || deck.file == null) {
            return;
        }
        if (!on) {
            stopSound();
            if (deck.mode == Tape.Mode.PLAY) {
                deck.mode = Tape.Mode.PAUSE;
            }
        } else if (deck.mode == Tape.Mode.PAUSE || deck.mode == Tape.Mode.STOP) {
            if (deck.mode == Tape.Mode.STOP) {
                deck.pos = 0; // At the end: from the start again
            }
            deck.mode = Tape.Mode.PLAY;
        }
        shownFrame = Integer.MIN_VALUE;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel sl) || cassette.isEmpty()) {
            return;
        }
        Audio audio = Audio.get();
        if (deck.mode == Tape.Mode.PLAY && on() && audio != null && (sound == null || !audio.playing(sound))) {
            sound = audio.play(sl, worldPosition.getCenter(), VcrBlockEntity.RANGE, deck::frame);
        }
        if (sound == null || audio == null || !audio.playing(sound)) {
            deck.advance();
        }
        long now = sl.getGameTime();
        if (now % 2 != 0) {
            return;
        }
        int index;
        byte[] data = null;
        if (!Video.isVideo(deck.file) || deck.mode == Tape.Mode.STOP) {
            index = TvNet.BLUE;
        } else if (Video.prepare(deck.file) == Video.State.FAILED) {
            index = TvNet.FAILED;
            data = Video.why(deck.file).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } else if (Video.prepare(deck.file) != Video.State.READY) {
            index = TvNet.LOADING;
            data = new byte[] {(byte) Video.progress(deck.file)};
        } else {
            index = Video.index(deck.pos);
        }
        if (index == shownFrame && now - shownAt < (index == TvNet.LOADING ? 20 : 60)) {
            return;
        }
        if (index >= 0) {
            data = Video.frame(deck.file, index);
            if (data == null) {
                index = TvNet.BLUE;
            }
        }
        shownFrame = index;
        shownAt = now;
        TvNet.show(sl, worldPosition, index, data);
    }

    private void stopSound() {
        Audio audio = Audio.get();
        if (audio != null && sound != null) {
            audio.stop(sound);
        }
        sound = null;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        stopSound();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        stopSound();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!cassette.isEmpty()) {
            tag.put("Cassette", cassette.save(registries));
        }
        tag.putLong("Pos", deck.pos);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cassette = tag.contains("Cassette") ? ItemStack.parseOptional(registries, tag.getCompound("Cassette")) : ItemStack.EMPTY;
        deck.file = cassette.isEmpty() ? null : cassette.get(GearRegistry.TAPE.get());
        deck.pos = tag.getLong("Pos");
        // After a restart the tape waits, paused, where it was
        deck.mode = deck.file == null ? Tape.Mode.STOP : Tape.Mode.PAUSE;
    }
}
