package com.waylays.arcanes.client;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.AurumBladeRenderer;
import com.waylays.arcanes.client.render.EldoradoSunRenderer;
import com.waylays.arcanes.client.render.GildedChainsRenderer;
import com.waylays.arcanes.client.render.GoldItemProjectileRenderer;
import com.waylays.arcanes.client.render.GoldSpikeRenderer;
import com.waylays.arcanes.client.render.GoldenSentinelRenderer;
import com.waylays.arcanes.client.render.MoltenPoolRenderer;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModItems;
import io.redspace.ironsspellbooks.render.SpellBookCurioRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GOLD_BOLT.get(),
                context -> new GoldItemProjectileRenderer<>(context, new ItemStack(Items.GOLD_INGOT), .9f, 1.15f, 35f));
        event.registerEntityRenderer(ModEntities.GOLDFALL_INGOT.get(),
                context -> new GoldItemProjectileRenderer<>(context, new ItemStack(Items.GOLD_INGOT), 2.6f, 2.2f, 8f));
        event.registerEntityRenderer(ModEntities.MOLTEN_GOLD_BLOB.get(),
                context -> new GoldItemProjectileRenderer<>(context, new ItemStack(Items.GOLD_NUGGET), .45f, .8f, 50f));
        event.registerEntityRenderer(ModEntities.GILDED_CHAINS.get(), GildedChainsRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLD_SPIKE.get(), GoldSpikeRenderer::new);
        event.registerEntityRenderer(ModEntities.AURUM_BLADE.get(), AurumBladeRenderer::new);
        event.registerEntityRenderer(ModEntities.MOLTEN_POOL.get(), MoltenPoolRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLDEN_SENTINEL.get(), GoldenSentinelRenderer::new);
        event.registerEntityRenderer(ModEntities.ELDORADO_SUN.get(), EldoradoSunRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Spellbooks hang on the player's hip like Iron's own
        event.enqueueWork(() -> CuriosRendererRegistry.register(ModItems.GOLDEN_GRIMOIRE.get(), SpellBookCurioRenderer::new));
    }
}
