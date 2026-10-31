package com.waylays.arcanes.ritual;

import com.waylays.arcanes.registry.ModBlocks;
import com.waylays.arcanes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** What happens when a ritual is chosen in an Altar of Rites' book (on the server). */
public final class AltarOfRites {
    private AltarOfRites() {}

    /**
     * {@code player} chose ritual {@code index} at the altar at {@code pos}: an altar ritual is prepared round it (its
     * offering taken, but not from a creative player); another sets the Ritual Chalk the player holds.
     */
    public static void choose(ServerPlayer player, BlockPos pos, int index) {
        ServerLevel level = player.serverLevel();
        if (index < 0 || index >= Ritual.values().length || !level.isLoaded(pos) || !level.getBlockState(pos).is(ModBlocks.ALTAR_OF_RITES.get())
                || player.distanceToSqr(Vec3.atCenterOf(pos)) > 8 * 8) {
            return;
        }
        Ritual ritual = Ritual.byIndex(index);
        if (ritual.surface != Ritual.Surface.ALTAR) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = player.getItemInHand(hand);
                if (held.is(ModItems.RITUAL_CHALK.get())) {
                    RitualChalkItem.setRitual(held, ritual);
                    player.displayClientMessage(Component.translatable("ui.arcanes_oublies.ritual_chosen", ritual.title(), ritual.participants)
                            .withStyle(s -> s.withColor(ritual.color)), true);
                    level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1, 1);
                    return;
                }
            }
            player.displayClientMessage(Component.translatable("ui.arcanes_oublies.altar.need_chalk").withStyle(ChatFormatting.GOLD), true);
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(pos);
        if (!level.getEntitiesOfClass(RitualCircle.class, new AABB(at, at).inflate(RitualCircle.RADIUS * 2.5)).isEmpty()) {
            player.displayClientMessage(Component.translatable("ui.arcanes_oublies.ritual_too_close").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!takeOffering(player, ritual)) {
            player.displayClientMessage(Component.translatable("ui.arcanes_oublies.altar.need_offering", ritual.offeringCount,
                    ritual.offering.getDefaultInstance().getHoverName()).withStyle(ChatFormatting.RED), true);
            return;
        }
        // Drawn round the altar, turned the way the player looks (the Bridge's path leads off that way)
        RitualCircle.draw(level, player, ritual, at, player.getYRot(), null, null);
        player.displayClientMessage(Component.translatable("ui.arcanes_oublies.altar.prepared", ritual.title()).withStyle(s -> s.withColor(ritual.color)), true);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.2f, .7f);
        level.sendParticles(ParticleTypes.ENCHANT, at.x, at.y + 1.3, at.z, 40, .6, .4, .6, .6);
    }

    /** Takes the ritual's offering from the player's inventory, if they have it all (a creative player gives nothing). */
    private static boolean takeOffering(Player player, Ritual ritual) {
        if (ritual.offering == null || ritual.offeringCount <= 0 || player.getAbilities().instabuild) {
            return true;
        }
        var inventory = player.getInventory();
        int have = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ritual.offering)) {
                have += stack.getCount();
            }
        }
        if (have < ritual.offeringCount) {
            return false;
        }
        int left = ritual.offeringCount;
        for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ritual.offering)) {
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
        }
        inventory.setChanged();
        return true;
    }
}
