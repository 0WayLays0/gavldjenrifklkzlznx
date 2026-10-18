package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.BlastLeapEffect;
import com.waylays.arcanes.effect.GildedArmorEffect;
import com.waylays.arcanes.effect.MidasCurseEffect;
import com.waylays.arcanes.effect.ParchmentAegisEffect;
import com.waylays.arcanes.effect.ReactiveShieldEffect;
import com.waylays.arcanes.effect.SilenceEffect;
import com.waylays.arcanes.effect.VolatileBrandEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ArcanesOublies.MODID);

    public static final RegistryObject<MobEffect> MIDAS_CURSE = EFFECTS.register("midas_curse", MidasCurseEffect::new);
    public static final RegistryObject<MobEffect> GILDED_ARMOR = EFFECTS.register("gilded_armor", GildedArmorEffect::new);
    public static final RegistryObject<MobEffect> VOLATILE_BRAND = EFFECTS.register("volatile_brand", VolatileBrandEffect::new);
    public static final RegistryObject<MobEffect> BLAST_LEAP = EFFECTS.register("blast_leap", BlastLeapEffect::new);
    public static final RegistryObject<MobEffect> REACTIVE_SHIELD = EFFECTS.register("reactive_shield", ReactiveShieldEffect::new);
    public static final RegistryObject<MobEffect> SILENCE = EFFECTS.register("silence", SilenceEffect::new);
    public static final RegistryObject<MobEffect> PARCHMENT_AEGIS = EFFECTS.register("parchment_aegis", ParchmentAegisEffect::new);

    private ModEffects() {}
}
