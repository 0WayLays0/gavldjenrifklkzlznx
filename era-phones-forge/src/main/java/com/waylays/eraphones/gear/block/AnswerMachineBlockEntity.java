package com.waylays.eraphones.gear.block;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.Audio;
import com.waylays.eraphones.registry.GearRegistry;
import com.waylays.eraphones.server.AnswerMachines;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The answering machine's line and its messages: who called, when, how long, what they typed, and their voice (kept
 * in the world's folder, eraphones/repondeur, one file a message). Played out of the machine's little speaker.
 */
public class AnswerMachineBlockEntity extends BlockEntity {
    /** The most messages a tape holds. */
    public static final int MAX = 20;
    private String number = "";
    private final List<Message> messages = new ArrayList<>();
    private Object playing;

    record Message(String from, long at, int seconds, String text, String file) {}

    public AnswerMachineBlockEntity(BlockPos pos, BlockState state) {
        super(GearRegistry.MACHINE_BE.get(), pos, state);
    }

    public String number() {
        return number;
    }

    private Path folder() {
        return ((ServerLevel) level).getServer().getWorldPath(LevelResource.ROOT).resolve("eraphones").resolve("repondeur");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel sl && !number.isEmpty()) {
            AnswerMachines.put(number, GlobalPos.of(sl.dimension(), worldPosition));
        }
    }

    private void forget() {
        if (level != null && !level.isClientSide && !number.isEmpty()) {
            AnswerMachines.remove(number, GlobalPos.of(level.dimension(), worldPosition));
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

    public void link(String n, ServerPlayer p) {
        forget();
        number = n;
        AnswerMachines.put(number, GlobalPos.of(level.dimension(), worldPosition));
        setChanged();
        p.displayClientMessage(Component.translatable("machine.eraphones.linked", n), true);
        level.playSound(null, worldPosition, GearRegistry.MACHINE_BEEP.get(), SoundSource.BLOCKS, .5f, 1.4f);
    }

    /** Picked up a call (its little motor turns): only a sound here. */
    public void answering(boolean on) {
        if (level != null) {
            level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.UI_LOOM_SELECT_PATTERN, SoundSource.BLOCKS, .5f, .7f);
        }
    }

    /** A message left: kept, the light set blinking. */
    public void keep(String from, long at, short[] pcm, String text) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        String file = "";
        if (pcm.length > 4800) {
            file = UUID.randomUUID() + ".pcm";
            try {
                Files.createDirectories(folder());
                ByteBuffer b = ByteBuffer.allocate(pcm.length * 2).order(ByteOrder.LITTLE_ENDIAN);
                b.asShortBuffer().put(pcm);
                Files.write(folder().resolve(file), b.array());
            } catch (IOException e) {
                EraPhones.LOGGER.warn("A message could not be kept: {}", e.toString());
                file = "";
            }
        }
        if (file.isEmpty() && text.isBlank()) {
            return; // Hung up without a word
        }
        while (messages.size() >= MAX) {
            delete(messages.remove(0));
        }
        messages.add(new Message(from, at, pcm.length / Audio.RATE, text, file));
        level.setBlock(worldPosition, getBlockState().setValue(AnswerMachineBlock.NEW, true), 3);
        setChanged();
    }

    private void delete(Message m) {
        if (!m.file().isEmpty()) {
            try {
                Files.deleteIfExists(folder().resolve(m.file()));
            } catch (IOException ignored) {
                // Gone already
            }
        }
    }

    public void erase(ServerPlayer p) {
        for (Message m : messages) {
            delete(m);
        }
        messages.clear();
        stop();
        level.setBlock(worldPosition, getBlockState().setValue(AnswerMachineBlock.NEW, false), 3);
        setChanged();
        p.displayClientMessage(Component.translatable("machine.eraphones.erased"), true);
        level.playSound(null, worldPosition, GearRegistry.MACHINE_BEEP.get(), SoundSource.BLOCKS, .5f, .7f);
    }

    private void stop() {
        Audio audio = Audio.get();
        if (audio != null && playing != null) {
            audio.stop(playing);
        }
        playing = null;
    }

    /** The messages, one after the other, a beep before each; told in the chat as well. */
    public void play(ServerPlayer p) {
        Audio audio = Audio.get();
        if (audio != null && playing != null && audio.playing(playing)) {
            stop();
            return;
        }
        if (number.isEmpty()) {
            p.displayClientMessage(Component.translatable("machine.eraphones.unlinked"), true);
            return;
        }
        if (messages.isEmpty()) {
            p.displayClientMessage(Component.translatable("machine.eraphones.none", number), true);
            level.playSound(null, worldPosition, GearRegistry.MACHINE_BEEP.get(), SoundSource.BLOCKS, .5f, 1f);
            return;
        }
        p.sendSystemMessage(Component.translatable("machine.eraphones.count", messages.size(), number).withStyle(ChatFormatting.GOLD));
        List<short[]> sounds = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            long minutes = Math.max(0, (now - m.at()) / 60000);
            p.sendSystemMessage(Component.translatable("machine.eraphones.message", i + 1, m.from(), minutes, m.seconds())
                    .withStyle(ChatFormatting.GRAY));
            if (!m.text().isBlank()) {
                p.sendSystemMessage(Component.literal("  « " + m.text() + " »").withStyle(ChatFormatting.WHITE));
            }
            sounds.add(beep());
            if (!m.file().isEmpty()) {
                try {
                    byte[] raw = Files.readAllBytes(folder().resolve(m.file()));
                    short[] pcm = new short[raw.length / 2];
                    ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcm);
                    sounds.add(pcm);
                } catch (IOException e) {
                    EraPhones.LOGGER.warn("A message could not be read: {}", e.toString());
                }
            }
        }
        sounds.add(beep());
        level.setBlock(worldPosition, getBlockState().setValue(AnswerMachineBlock.NEW, false), 3);
        if (audio == null) {
            return; // Without Simple Voice Chat: the chat tells them, no voice
        }
        int total = 0;
        for (short[] s : sounds) {
            total += s.length;
        }
        short[] all = new short[total];
        int at = 0;
        for (short[] s : sounds) {
            System.arraycopy(s, 0, all, at, s.length);
            at += s.length;
        }
        // Through the machine's small speaker: a narrow, slightly hissing sound
        for (int i = 1; i < all.length; i++) {
            all[i] = (short) Math.max(-32768, Math.min(32767, all[i] * .7 + all[i - 1] * .25));
        }
        int[] pos = {0};
        playing = audio.play((ServerLevel) level, worldPosition.getCenter(), 10, () -> {
            if (pos[0] >= all.length) {
                return null;
            }
            short[] f = new short[Audio.FRAME];
            int n = Math.min(Audio.FRAME, all.length - pos[0]);
            System.arraycopy(all, pos[0], f, 0, n);
            pos[0] += n;
            return f;
        });
    }

    /** The machine's beep: half a second at 1 kHz, a little silence after. */
    private static short[] beep() {
        short[] s = new short[Audio.RATE * 7 / 10];
        for (int i = 0; i < Audio.RATE / 2; i++) {
            double env = Math.min(1, Math.min(i, Audio.RATE / 2 - i) / 240.0);
            s[i] = (short) (Math.sin(2 * Math.PI * 1000 * i / Audio.RATE) * 9000 * env);
        }
        return s;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Number", number);
        ListTag list = new ListTag();
        for (Message m : messages) {
            CompoundTag t = new CompoundTag();
            t.putString("From", m.from());
            t.putLong("At", m.at());
            t.putInt("Seconds", m.seconds());
            t.putString("Text", m.text());
            t.putString("File", m.file());
            list.add(t);
        }
        tag.put("Messages", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        number = tag.getString("Number");
        messages.clear();
        for (Tag t : tag.getList("Messages", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            messages.add(new Message(c.getString("From"), c.getLong("At"), c.getInt("Seconds"), c.getString("Text"), c.getString("File")));
        }
    }
}
