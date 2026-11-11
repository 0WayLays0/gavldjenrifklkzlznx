package com.waylays.eraphones.lock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The badges a card reader knows; the doors it holds open, and for how long. */
public class CardReaderBlockEntity extends BlockEntity {
    /** How long the doors stay open (ticks), and the light stays lit. */
    private static final int OPEN = 100, LIT = 30;
    private final Set<String> badges = new LinkedHashSet<>();
    private final List<BlockPos> opened = new ArrayList<>();
    private int closeIn, lightOff;

    public CardReaderBlockEntity(BlockPos pos, BlockState state) {
        super(LockRegistry.READER_BE.get(), pos, state);
    }

    void swipe(ServerPlayer p, ItemStack badge) {
        String id = BadgeItem.id(badge);
        ServerLevel level = (ServerLevel) this.level;
        if (p.isCreative() && id != null) {
            // A game master teaching it (or making it forget) this badge
            boolean added = badges.add(id);
            if (!added) {
                badges.remove(id);
            }
            setChanged();
            p.displayClientMessage(Component.translatable(added ? "lock.eraphones.reader_learned" : "lock.eraphones.reader_forgot", badge.getHoverName())
                    .withStyle(ChatFormatting.GOLD), true);
            beep(level, true);
            return;
        }
        boolean ok = id != null && badges.contains(id);
        beep(level, ok);
        if (ok) {
            openDoors(level);
        } else {
            p.displayClientMessage(Component.translatable("lock.eraphones.reader_denied").withStyle(ChatFormatting.DARK_RED), true);
        }
    }

    void info(ServerPlayer p) {
        if (p.isCreative() && p.isSecondaryUseActive()) {
            badges.clear();
            setChanged();
            p.displayClientMessage(Component.translatable("lock.eraphones.reader_cleared").withStyle(ChatFormatting.GOLD), true);
        } else if (p.isCreative()) {
            p.displayClientMessage(Component.translatable("lock.eraphones.reader_info", badges.size()).withStyle(ChatFormatting.GRAY), true);
        } else {
            p.displayClientMessage(Component.translatable("lock.eraphones.reader_needs").withStyle(ChatFormatting.GRAY), true);
        }
    }

    private void beep(ServerLevel level, boolean ok) {
        level.setBlock(worldPosition, getBlockState().setValue(CardReaderBlock.LIGHT, ok ? 1 : 2), 3);
        lightOff = LIT;
        if (ok) {
            level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, .6f, 1.6f);
            level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, .6f, 2f);
        } else {
            level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.BLOCKS, .6f, .6f);
        }
    }

    /** The doors, gates and trapdoors within two blocks open (the lower half of each door). */
    private void openDoors(ServerLevel level) {
        for (BlockPos q : BlockPos.betweenClosed(worldPosition.offset(-2, -2, -2), worldPosition.offset(2, 2, 2))) {
            BlockState s = level.getBlockState(q);
            boolean door = s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
            boolean other = s.getBlock() instanceof FenceGateBlock || s.getBlock() instanceof TrapDoorBlock;
            if ((door || other) && !s.getValue(BlockStateProperties.OPEN)) {
                if (door) {
                    ((DoorBlock) s.getBlock()).setOpen(null, level, s, q, true);
                } else {
                    level.setBlock(q, s.setValue(BlockStateProperties.OPEN, true), 10);
                    level.playSound(null, q, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, .8f, 1);
                }
                opened.add(q.immutable());
            }
        }
        closeIn = OPEN;
    }

    void tick(ServerLevel level) {
        if (lightOff > 0 && --lightOff == 0) {
            level.setBlock(worldPosition, getBlockState().setValue(CardReaderBlock.LIGHT, 0), 3);
        }
        if (closeIn > 0 && --closeIn == 0) {
            for (BlockPos q : opened) {
                BlockState s = level.getBlockState(q);
                if (s.getBlock() instanceof DoorBlock d) {
                    d.setOpen(null, level, s, q, false);
                } else if (s.hasProperty(BlockStateProperties.OPEN)) {
                    level.setBlock(q, s.setValue(BlockStateProperties.OPEN, false), 10);
                    level.playSound(null, q, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, .8f, 1);
                }
            }
            opened.clear();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (String b : badges) {
            list.add(StringTag.valueOf(b));
        }
        tag.put("Badges", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        badges.clear();
        for (Tag t : tag.getList("Badges", Tag.TAG_STRING)) {
            badges.add(t.getAsString());
        }
    }
}
