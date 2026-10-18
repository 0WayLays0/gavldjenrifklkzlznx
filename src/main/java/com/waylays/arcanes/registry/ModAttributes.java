package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.attribute.MagicRangedAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Per-school power and resistance attributes, built like Iron's own (default 1.0 = 100%).
 */
@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ArcanesOublies.MODID);

    public static final RegistryObject<Attribute> GOLD_SPELL_POWER = powerAttribute("gold");
    public static final RegistryObject<Attribute> GOLD_MAGIC_RESIST = resistanceAttribute("gold");
    public static final RegistryObject<Attribute> EXPLOSION_SPELL_POWER = powerAttribute("explosion");
    public static final RegistryObject<Attribute> EXPLOSION_MAGIC_RESIST = resistanceAttribute("explosion");

    private ModAttributes() {}

    @SubscribeEvent
    public static void addToEntities(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> ATTRIBUTES.getEntries().forEach(attribute -> event.add(type, attribute.get())));
    }

    private static RegistryObject<Attribute> powerAttribute(String school) {
        String name = school + "_spell_power";
        return ATTRIBUTES.register(name, () -> new MagicRangedAttribute("attribute." + ArcanesOublies.MODID + "." + name, 1.0D, -100, 100).setSyncable(true));
    }

    private static RegistryObject<Attribute> resistanceAttribute(String school) {
        String name = school + "_magic_resist";
        return ATTRIBUTES.register(name, () -> new MagicRangedAttribute("attribute." + ArcanesOublies.MODID + "." + name, 1.0D, -100, 100).setSyncable(true));
    }
}
