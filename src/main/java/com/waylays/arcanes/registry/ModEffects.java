package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.GildedArmorEffect;
import com.waylays.arcanes.effect.MidasCurseEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ArcanesOublies.MODID);

    public static final RegistryObject<MobEffect> MIDAS_CURSE = EFFECTS.register("midas_curse", MidasCurseEffect::new);
    public static final RegistryObject<MobEffect> GILDED_ARMOR = EFFECTS.register("gilded_armor", GildedArmorEffect::new);

    private ModEffects() {}
}
