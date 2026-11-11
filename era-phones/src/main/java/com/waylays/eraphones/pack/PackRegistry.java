package com.waylays.eraphones.pack;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.registry.EraRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** The backpacks (eight of them), their open menu, their creative tab. */
public final class PackRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, EraPhones.MODID);

    /**
     * The backpacks: the red school bag, the denim one, the army canvas one, the brown leather rucksack, the yellow one
     * covered in patches, the purple and pink one, the black sports bag, the big orange hiking pack with its bedroll.
     */
    public static final String[] STYLES = {"sac_a_dos_rouge", "sac_a_dos_jean", "sac_a_dos_militaire", "sac_a_dos_cuir", "sac_a_dos_jaune",
            "sac_a_dos_violet", "sac_a_dos_sport", "sac_a_dos_rando"};
    /** Each one's colour, for its open view's band. */
    public static final int[] COLORS = {0xFFB8302A, 0xFF4A6A9C, 0xFF5E6B3A, 0xFF7A4A26, 0xFFE0B23A, 0xFF8A4AA8, 0xFF2A2A2E, 0xFFD8702A};
    public static final List<DeferredHolder<Item, Item>> BACKPACKS = new ArrayList<>();

    static {
        for (int i = 0; i < STYLES.length; i++) {
            int style = i;
            BACKPACKS.add(EraRegistry.ITEMS.register(STYLES[i], () -> new BackpackItem(style, new Item.Properties().stacksTo(1))));
        }
    }

    public static final DeferredHolder<MenuType<?>, MenuType<BackpackMenu>> MENU = MENUS.register("sac_a_dos",
            () -> IMenuTypeExtension.create(BackpackMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = EraRegistry.TABS.register("sacs_a_dos", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.eraphones.sacs_a_dos"))
            .icon(() -> new ItemStack(BACKPACKS.get(0).get()))
            .withTabsBefore(com.waylays.eraphones.event.EventRegistry.TAB.getId())
            .displayItems((p, out) -> {
                for (DeferredHolder<Item, Item> b : BACKPACKS) {
                    out.accept(b.get());
                }
            }).build());

    private PackRegistry() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
