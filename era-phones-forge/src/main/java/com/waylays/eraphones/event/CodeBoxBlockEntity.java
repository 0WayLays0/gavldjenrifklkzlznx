package com.waylays.eraphones.event;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.waylays.eraphones.compat.PacketDistributor;


/** The strongbox's code and the nine things it can hold. */
public class CodeBoxBlockEntity extends BlockEntity {
    public static final int MIN_DIGITS = 3, MAX_DIGITS = 6;
    private String code = "0000";
    private final SimpleContainer items = new SimpleContainer(9) {
        @Override
        public void setChanged() {
            super.setChanged();
            CodeBoxBlockEntity.this.setChanged();
        }
    };

    public CodeBoxBlockEntity(BlockPos pos, BlockState state) {
        super(EventRegistry.CODE_BOX_BE.get(), pos, state);
    }

    public SimpleContainer items() {
        return items;
    }

    public String code() {
        return code;
    }

    public boolean open() {
        return getBlockState().getValue(CodeBoxBlock.OPEN);
    }

    public static boolean validCode(String c) {
        return c != null && c.length() >= MIN_DIGITS && c.length() <= MAX_DIGITS && c.chars().allMatch(Character::isDigit);
    }

    public void setCode(String c) {
        if (validCode(c)) {
            code = c;
            setChanged();
        }
    }

    public void setOpen(boolean open) {
        if (level != null) {
            level.setBlock(worldPosition, getBlockState().setValue(CodeBoxBlock.OPEN, open), 3);
        }
    }

    /** Used: locked, its wheels to turn (a game master crouching in creative fills it instead); open, what it holds. */
    public void use(ServerPlayer p) {
        if (p.isCreative() && p.isSecondaryUseActive()) {
            showInside(p);
            p.sendSystemMessage(Component.translatable("event.eraphones.codebox.staff_hint", code).withStyle(ChatFormatting.GRAY));
            return;
        }
        if (open()) {
            showInside(p);
            return;
        }
        PacketDistributor.sendToPlayer(p, new EventNet.OpenCode(worldPosition, code.length(), p.isCreative() ? code : ""));
    }

    private void showInside(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, inv, items, 1),
                Component.translatable("block.eraphones.boite_code")));
    }

    /** {@code p} tried {@code tried} on the wheels. */
    public void attempt(ServerPlayer p, String tried) {
        if (open() || level == null) {
            return;
        }
        if (!code.equals(tried)) {
            level.playSound(null, worldPosition, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, .8f, .8f);
            p.displayClientMessage(Component.translatable("event.eraphones.codebox.wrong").withStyle(ChatFormatting.DARK_RED), true);
            return;
        }
        setOpen(true);
        level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, .9f, .7f);
        level.playSound(null, worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, .6f, .6f);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, 8, .2, .1, .2, .01);
        }
        p.displayClientMessage(Component.translatable("event.eraphones.codebox.opened").withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Code", code);
        NonNullList<ItemStack> list = NonNullList.withSize(items.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) {
            list.set(i, items.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (validCode(tag.getString("Code"))) {
            code = tag.getString("Code");
        }
        NonNullList<ItemStack> list = NonNullList.withSize(items.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, list);
        for (int i = 0; i < list.size(); i++) {
            items.setItem(i, list.get(i));
        }
    }
}
