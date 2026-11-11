package com.waylays.eraphones.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/** A registered thing, as NeoForge names it: a Forge RegistryObject (or, for item data, the thing itself). */
public class DeferredHolder<R, T extends R> implements Supplier<T> {
    private final RegistryObject<T> object;
    private final T value;
    private final ResourceLocation id;

    DeferredHolder(RegistryObject<T> object) {
        this.object = object;
        this.value = null;
        this.id = object.getId();
    }

    DeferredHolder(ResourceLocation id, T value) {
        this.object = null;
        this.value = value;
        this.id = id;
    }

    @Override
    public T get() {
        return object != null ? object.get() : value;
    }

    public T value() {
        return get();
    }

    public ResourceLocation getId() {
        return id;
    }

    public boolean isBound() {
        return object == null || object.isPresent();
    }
}
