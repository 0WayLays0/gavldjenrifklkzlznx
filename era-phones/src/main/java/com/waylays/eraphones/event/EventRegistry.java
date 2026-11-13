package com.waylays.eraphones.event;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.registry.EraRegistry;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** The photographs pinned to a wall or laid on a table. */
public final class EventRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EraPhones.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EraPhones.MODID);

    /** A photograph pinned to a wall or laid on a table (put there from the photograph itself, so it has no item). */
    public static final DeferredBlock<com.waylays.eraphones.gear.PinnedPhotoBlock> PINNED_PHOTO = BLOCKS.register("photo_epinglee",
            () -> new com.waylays.eraphones.gear.PinnedPhotoBlock(BlockBehaviour.Properties.of().instabreak().sound(SoundType.WOOL).noOcclusion()
                    .noCollission().pushReaction(PushReaction.DESTROY)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.waylays.eraphones.gear.PinnedPhotoBlockEntity>> PINNED_PHOTO_BE =
            BLOCK_ENTITIES.register("photo_epinglee", () -> BlockEntityType.Builder.of(com.waylays.eraphones.gear.PinnedPhotoBlockEntity::new,
                    PINNED_PHOTO.get()).build(null));

    private EventRegistry() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
