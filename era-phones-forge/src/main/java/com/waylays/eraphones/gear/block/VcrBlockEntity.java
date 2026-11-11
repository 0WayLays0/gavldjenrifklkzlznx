package com.waylays.eraphones.gear.block;

import com.waylays.eraphones.gear.Audio;
import com.waylays.eraphones.gear.CassetteItem;
import com.waylays.eraphones.gear.Tape;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A video recorder's cassette and deck. The deck's sound is played through Simple Voice Chat out of the recorder
 * (16 blocks round); where the tape is, and what it does, is sent to the players near it twice a second.
 */
public class VcrBlockEntity extends BlockEntity {
    public static final float RANGE = 16;
    /** How far a television may be from the recorder to show its tape (blocks). */
    public static final int TV_REACH = 4;
    private ItemStack cassette = ItemStack.EMPTY;
    private final Tape.Deck deck = new Tape.Deck();
    private Object sound;
    private long shownPos = -1;
    private Tape.Mode shownMode;
    /** On the players' side: the counter and the mode as last sent. */
    private long clientPos;
    private String clientMode = "STOP";

    public VcrBlockEntity(BlockPos pos, BlockState state) {
        super(GearRegistry.VCR_BE.get(), pos, state);
    }

    public ItemStack cassette() {
        return cassette;
    }

    public long position() {
        return level != null && level.isClientSide ? clientPos : deck.pos;
    }

    public String mode() {
        return level != null && level.isClientSide ? clientMode : deck.mode.name();
    }

    /** Takes the cassette out, the tape left where it was wound to. */
    public ItemStack takeCassette() {
        stopSound();
        ItemStack out = cassette;
        if (!out.isEmpty()) {
            GearRegistry.TAPE_POS.get().set(out, deck.pos);
        }
        cassette = ItemStack.EMPTY;
        deck.file = null;
        deck.mode = Tape.Mode.STOP;
        deck.pos = 0;
        changed();
        return out;
    }

    public void press(ServerPlayer p, String key) {
        ServerLevel level = (ServerLevel) this.level;
        if (level == null) {
            return;
        }
        switch (key) {
            case "insert" -> {
                if (!cassette.isEmpty()) {
                    p.displayClientMessage(Component.translatable("gear.eraphones.vcr_full").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                InteractionHand hand = p.getMainHandItem().getItem() instanceof CassetteItem ? InteractionHand.MAIN_HAND
                        : p.getOffhandItem().getItem() instanceof CassetteItem ? InteractionHand.OFF_HAND : null;
                if (hand == null) {
                    p.displayClientMessage(Component.translatable("gear.eraphones.vcr_no_tape").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                cassette = p.getItemInHand(hand).split(1);
                deck.file = GearRegistry.TAPE.get().get(cassette);
                deck.pos = GearRegistry.TAPE_POS.get().getOrDefault(cassette, 0L);
                deck.mode = Tape.Mode.STOP;
                if (deck.file != null) {
                    Tape.sound(deck.file);
                }
                click(level, .8f);
            }
            case "eject" -> {
                if (cassette.isEmpty()) {
                    return;
                }
                ItemStack out = takeCassette();
                if (!p.getInventory().add(out)) {
                    p.drop(out, false);
                }
                click(level, .7f);
                level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, .35f, 1.8f);
            }
            case "play", "rewind", "forward" -> {
                if (cassette.isEmpty() || deck.file == null) {
                    p.displayClientMessage(Component.translatable(cassette.isEmpty() ? "gear.eraphones.vcr_empty" : "gear.eraphones.vcr_blank")
                            .withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                if (Audio.get() == null) {
                    p.displayClientMessage(Component.translatable("gear.eraphones.needs_voicechat").withStyle(ChatFormatting.GRAY), true);
                }
                deck.mode = key.equals("play") ? Tape.Mode.PLAY : key.equals("rewind") ? Tape.Mode.REWIND : Tape.Mode.FORWARD;
                startSound(level);
                click(level, 1.1f);
            }
            case "pause" -> {
                if (deck.mode == Tape.Mode.PLAY) {
                    deck.mode = Tape.Mode.PAUSE;
                } else if (deck.mode == Tape.Mode.PAUSE) {
                    deck.mode = Tape.Mode.PLAY;
                    startSound(level);
                }
                click(level, 1.3f);
            }
            case "stop" -> {
                deck.mode = Tape.Mode.STOP;
                click(level, 1f);
            }
            default -> {
            }
        }
        changed();
    }

    private void click(ServerLevel level, float pitch) {
        level.playSound(null, worldPosition, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, .4f, pitch);
    }

    private void startSound(ServerLevel level) {
        Audio audio = Audio.get();
        if (audio == null || (sound != null && audio.playing(sound))) {
            return;
        }
        sound = audio.play(level, worldPosition.getCenter(), RANGE, deck::frame);
    }

    private void stopSound() {
        Audio audio = Audio.get();
        if (audio != null && sound != null) {
            audio.stop(sound);
        }
        sound = null;
    }

    /** The televisions beside the recorder (looked for every two seconds), and what was last shown on them. */
    private final java.util.List<BlockPos> tvs = new java.util.ArrayList<>();
    private int shownFrame = Integer.MIN_VALUE;
    private long tvAt;

    public void serverTick() {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        // No power: the tape stops where it is
        if (now % 10 == 0 && deck.mode != Tape.Mode.STOP && deck.mode != Tape.Mode.PAUSE && com.waylays.eraphones.gear.Power.cut(level, worldPosition)) {
            deck.mode = Tape.Mode.PAUSE;
            stopSound();
            changed();
        }
        Audio audio = Audio.get();
        if (sound == null || audio == null || !audio.playing(sound)) {
            deck.advance();
        }
        if (now % 40 == 0) {
            findTvs();
        }
        if (now % 2 == 0 && !tvs.isEmpty()) {
            feedTvs(now);
        }
        if (now % 10 != 0) {
            return;
        }
        // The tape stopped by itself (an end reached): the sound has ended with it
        if (deck.mode != shownMode || Math.abs(deck.pos - shownPos) >= Audio.RATE / 2) {
            changed();
        }
    }

    private void findTvs() {
        tvs.clear();
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-TV_REACH, -2, -TV_REACH), worldPosition.offset(TV_REACH, 2, TV_REACH))) {
            if (level.getBlockEntity(p) instanceof TvBlockEntity tv && !tv.hasCassette()) {
                tvs.add(p.immutable());
            }
        }
    }

    /** Sends the frame under the heads to the televisions, when it changed (and now and then for whoever came near). */
    private void feedTvs(long now) {
        ServerLevel sl = (ServerLevel) level;
        String file = deck.file;
        int index;
        byte[] data = null;
        if (!com.waylays.eraphones.gear.Video.isVideo(file) || deck.mode == Tape.Mode.STOP && deck.pos == 0 && cassette.isEmpty()) {
            index = com.waylays.eraphones.gear.TvNet.BLUE;
        } else if (com.waylays.eraphones.gear.Video.prepare(file) == com.waylays.eraphones.gear.Video.State.FAILED) {
            index = com.waylays.eraphones.gear.TvNet.FAILED;
            data = com.waylays.eraphones.gear.Video.why(file).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } else if (com.waylays.eraphones.gear.Video.prepare(file) != com.waylays.eraphones.gear.Video.State.READY) {
            index = com.waylays.eraphones.gear.TvNet.LOADING;
            data = new byte[] {(byte) com.waylays.eraphones.gear.Video.progress(file)};
        } else if (deck.mode == Tape.Mode.STOP) {
            index = com.waylays.eraphones.gear.TvNet.BLUE;
        } else {
            index = com.waylays.eraphones.gear.Video.index(deck.pos);
        }
        if (index == shownFrame && now - tvAt < (index == com.waylays.eraphones.gear.TvNet.LOADING ? 20 : 60)) {
            return;
        }
        if (index >= 0) {
            data = com.waylays.eraphones.gear.Video.frame(file, index);
            if (data == null) {
                index = com.waylays.eraphones.gear.TvNet.BLUE;
            }
        }
        shownFrame = index;
        tvAt = now;
        for (BlockPos tv : tvs) {
            com.waylays.eraphones.gear.TvNet.show(sl, tv, index, data);
        }
    }

    private void changed() {
        shownPos = deck.pos;
        shownMode = deck.mode;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!cassette.isEmpty()) {
            tag.put("Cassette", cassette.save(new net.minecraft.nbt.CompoundTag()));
        }
        tag.putLong("Pos", deck.pos);
        tag.putString("Mode", deck.mode.name());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cassette = tag.contains("Cassette") ? ItemStack.of(tag.getCompound("Cassette")) : ItemStack.EMPTY;
        deck.file = cassette.isEmpty() ? null : GearRegistry.TAPE.get().get(cassette);
        deck.pos = tag.getLong("Pos");
        clientPos = deck.pos;
        clientMode = tag.getString("Mode").isEmpty() ? "STOP" : tag.getString("Mode");
        // After a restart the tape waits, stopped, where it was
        deck.mode = Tape.Mode.STOP;
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
