package com.waylays.eraphones.event;

import com.waylays.eraphones.horror.HorrorService;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** The strongbox's code, whether a balloon rises when it opens, and the nine things it can hold. */
public class CodeBoxBlockEntity extends BlockEntity {
    public static final int MIN_DIGITS = 3, MAX_DIGITS = 6;
    private String code = "0000";
    private boolean balloon = true;
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

    public boolean balloon() {
        return balloon;
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

    public void setBalloon(boolean b) {
        balloon = b;
        setChanged();
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
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> ChestMenu.oneRow(id, inv, items),
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
        if (balloon) {
            // A red balloon rising out of the box, seen by all who are near
            List<ServerPlayer> near = new ArrayList<>();
            for (ServerPlayer o : ((ServerLevel) level).players()) {
                if (o.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 48 * 48) {
                    near.add(o);
                }
            }
            HorrorService.show(near, "ballon", 360, "", Vec3.atCenterOf(worldPosition).add(0, .9, 0));
        }
        p.displayClientMessage(Component.translatable("event.eraphones.codebox.opened").withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Code", code);
        tag.putBoolean("Balloon", balloon);
        NonNullList<ItemStack> list = NonNullList.withSize(items.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) {
            list.set(i, items.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, list, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (validCode(tag.getString("Code"))) {
            code = tag.getString("Code");
        }
        balloon = !tag.contains("Balloon") || tag.getBoolean("Balloon");
        NonNullList<ItemStack> list = NonNullList.withSize(items.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, list, registries);
        for (int i = 0; i < list.size(); i++) {
            items.setItem(i, list.get(i));
        }
    }
}
