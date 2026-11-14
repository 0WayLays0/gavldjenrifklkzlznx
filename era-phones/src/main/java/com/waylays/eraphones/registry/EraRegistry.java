package com.waylays.eraphones.registry;

import com.mojang.serialization.Codec;
import com.waylays.eraphones.Era;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.item.ChargerItem;
import com.waylays.eraphones.item.PhoneItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/** The phones, the charger, the two things a phone carries (its number, its battery) and the creative tab. */
public final class EraRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, EraPhones.MODID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, EraPhones.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EraPhones.MODID);

    /** The phone's number, "555-0142", given the first time it is carried. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> NUMBER = COMPONENTS.register("number",
            () -> DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
    /** The battery, in thousandths of a full charge. */
    /**
     * The chance effects the weapons' workshop used to put on items, when it was part of this mod (it is in Arcanes
     * Oubliés now): still known, as it was saved, so that the items that carry them load; they do nothing here.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.mojang.serialization.Dynamic<?>>> OLD_CHANCE_EFFECTS =
            COMPONENTS.register("effets_chance", () -> DataComponentType.<com.mojang.serialization.Dynamic<?>>builder().persistent(Codec.PASSTHROUGH).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BATTERY = COMPONENTS.register("battery",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static final Map<Era, DeferredHolder<Item, Item>> PHONES = new EnumMap<>(Era.class);

    static {
        for (Era era : Era.values()) {
            Rarity rarity = era == Era.SMART ? Rarity.EPIC : era == Era.FLIP ? Rarity.RARE : Rarity.UNCOMMON;
            PHONES.put(era, ITEMS.register(era.item, () -> new PhoneItem(era, new Item.Properties().stacksTo(1).rarity(rarity))));
        }
    }

    public static final DeferredHolder<Item, Item> CHARGER = ITEMS.register("chargeur", () -> new ChargerItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("phones", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.eraphones.telephones"))
            .icon(() -> new ItemStack(PHONES.get(Era.SMART).get()))
            .displayItems((params, out) -> {
                for (Era era : Era.values()) {
                    out.accept(PHONES.get(era).get());
                }
                out.accept(CHARGER.get());
            })
            .build());

    private EraRegistry() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        COMPONENTS.register(bus);
        TABS.register(bus);
    }
}
