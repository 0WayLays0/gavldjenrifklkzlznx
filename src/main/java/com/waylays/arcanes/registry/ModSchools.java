package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Schools of magic added to Iron's school registry. Each school needs, besides this entry: a damage type json,
 * a focus item tag, power/resistance attributes, and the item/scroll_{@code <school>} and
 * item/affinity_ring_{@code <school>} models that Iron's looks up by school id.
 */
public final class ModSchools {
    public static final DeferredRegister<SchoolType> SCHOOLS = DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, ArcanesOublies.MODID);

    public static final ResourceLocation GOLD_RESOURCE = ArcanesOublies.id("gold");
    public static final TagKey<Item> GOLD_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("gold_focus"));
    public static final ResourceKey<DamageType> GOLD_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("gold_magic"));
    public static final int GOLD_COLOR = 0xFFC93C;

    public static final RegistryObject<SchoolType> GOLD = SCHOOLS.register(GOLD_RESOURCE.getPath(), () -> new SchoolType(
            GOLD_RESOURCE,
            GOLD_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".gold").withStyle(Style.EMPTY.withColor(GOLD_COLOR)),
            LazyOptional.of(ModAttributes.GOLD_SPELL_POWER::get),
            LazyOptional.of(ModAttributes.GOLD_MAGIC_RESIST::get),
            LazyOptional.of(() -> SoundEvents.ARMOR_EQUIP_GOLD),
            GOLD_MAGIC
    ));

    public static final ResourceLocation EXPLOSION_RESOURCE = ArcanesOublies.id("explosion");
    public static final TagKey<Item> EXPLOSION_FOCUS = TagKey.create(Registries.ITEM, ArcanesOublies.id("explosion_focus"));
    public static final ResourceKey<DamageType> EXPLOSION_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("explosion_magic"));
    public static final int EXPLOSION_COLOR = 0xFF6A2A;

    public static final RegistryObject<SchoolType> EXPLOSION = SCHOOLS.register(EXPLOSION_RESOURCE.getPath(), () -> new SchoolType(
            EXPLOSION_RESOURCE,
            EXPLOSION_FOCUS,
            Component.translatable("school." + ArcanesOublies.MODID + ".explosion").withStyle(Style.EMPTY.withColor(EXPLOSION_COLOR)),
            LazyOptional.of(ModAttributes.EXPLOSION_SPELL_POWER::get),
            LazyOptional.of(ModAttributes.EXPLOSION_MAGIC_RESIST::get),
            LazyOptional.of(() -> SoundEvents.FIRECHARGE_USE),
            EXPLOSION_MAGIC
    ));

    private ModSchools() {}
}
