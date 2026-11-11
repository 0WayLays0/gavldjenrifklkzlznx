package com.waylays.eraphones.compat;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.function.Supplier;

/** Registers the mod's things, as NeoForge names it, over Forge's DeferredRegister (item data: kept here). */
public class DeferredRegister<T> {
    private final net.minecraftforge.registries.DeferredRegister<T> forge;
    private final String modid;

    DeferredRegister(net.minecraftforge.registries.DeferredRegister<T> forge, String modid) {
        this.forge = forge;
        this.modid = modid;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String modid) {
        return new DeferredRegister<>(net.minecraftforge.registries.DeferredRegister.create(key, modid), modid);
    }

    public static Blocks createBlocks(String modid) {
        return new Blocks(modid);
    }

    /** Item data: not a registry on 1.20.1, only named. */
    public static Components createComponents(String modid) {
        return new Components(modid);
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> sup) {
        return new DeferredHolder<>(forge.register(name, sup));
    }

    public void register(IEventBus bus) {
        if (forge != null) {
            forge.register(bus);
        }
    }

    public static final class Blocks extends DeferredRegister<Block> {
        Blocks(String modid) {
            super(net.minecraftforge.registries.DeferredRegister.create(Registries.BLOCK, modid), modid);
        }

        @Override
        public <I extends Block> DeferredBlock<I> register(String name, Supplier<? extends I> sup) {
            return new DeferredBlock<>(super.forgeRegister(name, sup));
        }
    }

    <I extends T> net.minecraftforge.registries.RegistryObject<I> forgeRegister(String name, Supplier<? extends I> sup) {
        return forge.register(name, sup);
    }

    public static final class Components extends DeferredRegister<DataComponentType<?>> {
        Components(String modid) {
            super(null, modid);
        }

        @Override
        public <I extends DataComponentType<?>> DeferredHolder<DataComponentType<?>, I> register(String name, Supplier<? extends I> sup) {
            I c = sup.get();
            c.name(name);
            return new DeferredHolder<>(new ResourceLocation(Components.this.modidOf(), name), c);
        }

        private String modidOf() {
            return super.modid;
        }
    }
}
