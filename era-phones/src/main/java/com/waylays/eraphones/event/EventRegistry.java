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

/**
 * The things made for a horror event in town: the missing children's posters (many kinds, to stick on the walls), the
 * code box, the star fragment; and their creative tab.
 */
public final class EventRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EraPhones.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EraPhones.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, EraPhones.MODID);

    /**
     * The posters, by name: one child (six kinds of paper: new, yellowed, torn, washed by the rain, faded by thirty years
     * of sun, half torn away), two, three, four children side by side, heaps stuck one over another, and walls covered
     * all over (their edges meet, block after block).
     */
    public static final String[] POSTERS = {
            "affiche_disparu_1", "affiche_disparu_2", "affiche_disparu_3", "affiche_disparu_4", "affiche_disparu_5", "affiche_disparu_6",
            "affiches_disparus_2_a", "affiches_disparus_2_b", "affiches_disparus_3", "affiches_disparus_4",
            "affiches_disparus_tas_a", "affiches_disparus_tas_b", "affiches_disparus_mur_a", "affiches_disparus_mur_b", "affiches_disparus_mur_c"};
    public static final List<DeferredBlock<PosterBlock>> POSTER_BLOCKS = new ArrayList<>();
    public static final List<DeferredHolder<Item, Item>> POSTER_ITEMS = new ArrayList<>();

    static {
        for (String name : POSTERS) {
            DeferredBlock<PosterBlock> b = BLOCKS.register(name, () -> new PosterBlock(BlockBehaviour.Properties.of().strength(.2f)
                    .sound(SoundType.WOOL).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY)));
            POSTER_BLOCKS.add(b);
            POSTER_ITEMS.add(EraRegistry.ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties())));
        }
    }

    public static final DeferredBlock<CodeBoxBlock> CODE_BOX = BLOCKS.register("boite_code",
            () -> new CodeBoxBlock(BlockBehaviour.Properties.of().strength(-1f, 3600000f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CodeBoxBlockEntity>> CODE_BOX_BE = BLOCK_ENTITIES.register("boite_code",
            () -> BlockEntityType.Builder.of(CodeBoxBlockEntity::new, CODE_BOX.get()).build(null));
    public static final DeferredHolder<Item, Item> CODE_BOX_ITEM = EraRegistry.ITEMS.register("boite_code",
            () -> new BlockItem(CODE_BOX.get(), new Item.Properties()));

    public static final DeferredHolder<EntityType<?>, EntityType<StarFragment>> FRAGMENT = ENTITIES.register("fragment_etoile",
            () -> EntityType.Builder.<StarFragment>of(StarFragment::new, MobCategory.MISC).sized(.8f, 1.5f).clientTrackingRange(10)
                    .updateInterval(1).build("fragment_etoile"));
    public static final DeferredHolder<Item, Item> FRAGMENT_ITEM = EraRegistry.ITEMS.register("fragment_etoile",
            () -> new StarFragmentItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = EraRegistry.TABS.register("evenement", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.eraphones.evenement"))
            .icon(() -> new ItemStack(POSTER_ITEMS.get(0).get()))
            .withTabsBefore(GearRegistry.TAB_HORROR.getId())
            .displayItems((p, out) -> {
                out.accept(FRAGMENT_ITEM.get());
                out.accept(CODE_BOX_ITEM.get());
                out.accept(com.waylays.eraphones.lock.LockRegistry.PADLOCK.get());
                out.accept(com.waylays.eraphones.lock.LockRegistry.KEY_ITEM.get());
                out.accept(com.waylays.eraphones.lock.LockRegistry.READER_ITEM.get());
                out.accept(com.waylays.eraphones.lock.LockRegistry.BADGE_ITEM.get());
                for (DeferredHolder<Item, Item> i : POSTER_ITEMS) {
                    out.accept(i.get());
                }
            }).build());

    private EventRegistry() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
    }
}
