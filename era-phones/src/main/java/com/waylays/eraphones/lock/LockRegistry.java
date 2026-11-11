package com.waylays.eraphones.lock;

import com.mojang.serialization.Codec;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.registry.EraRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Padlocks and their keys, access badges and the card reader that opens doors for them. */
public final class LockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EraPhones.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EraPhones.MODID);

    /** The key's (and its padlocks') secret; a badge's own number. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> KEY = EraRegistry.COMPONENTS.register("cle",
            () -> DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BADGE = EraRegistry.COMPONENTS.register("badge",
            () -> DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

    public static final DeferredHolder<Item, Item> PADLOCK = EraRegistry.ITEMS.register("cadenas", () -> new PadlockItem(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> KEY_ITEM = EraRegistry.ITEMS.register("cle", () -> new KeyItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> BADGE_ITEM = EraRegistry.ITEMS.register("badge", () -> new BadgeItem(new Item.Properties().stacksTo(1)));

    public static final DeferredBlock<CardReaderBlock> READER = BLOCKS.register("lecteur_carte",
            () -> new CardReaderBlock(BlockBehaviour.Properties.of().strength(-1f, 3600000f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> s.getValue(CardReaderBlock.LIGHT) > 0 ? 4 : 1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CardReaderBlockEntity>> READER_BE = BLOCK_ENTITIES.register("lecteur_carte",
            () -> BlockEntityType.Builder.of(CardReaderBlockEntity::new, READER.get()).build(null));
    public static final DeferredHolder<Item, Item> READER_ITEM = EraRegistry.ITEMS.register("lecteur_carte",
            () -> new BlockItem(READER.get(), new Item.Properties()));

    private LockRegistry() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
