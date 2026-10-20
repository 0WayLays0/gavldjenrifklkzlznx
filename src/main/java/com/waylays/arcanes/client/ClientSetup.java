package com.waylays.arcanes.client;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.model.ManaCrowModel;
import com.waylays.arcanes.client.model.ManaRatModel;
import com.waylays.arcanes.client.model.ManaSerpentModel;
import com.waylays.arcanes.client.render.AegisPagesRenderer;
import com.waylays.arcanes.client.render.AurumBladeRenderer;
import com.waylays.arcanes.client.render.BlastEffectRenderer;
import com.waylays.arcanes.client.render.BookmarkRenderer;
import com.waylays.arcanes.client.render.CataclysmRenderer;
import com.waylays.arcanes.client.render.ChapterRuneRenderer;
import com.waylays.arcanes.client.render.EldoradoSunRenderer;
import com.waylays.arcanes.client.render.FinalChapterRenderer;
import com.waylays.arcanes.client.render.FlyingGrimoireRenderer;
import com.waylays.arcanes.client.render.FlyingStackRenderer;
import com.waylays.arcanes.client.render.GildedChainsRenderer;
import com.waylays.arcanes.client.render.GlowOrbRenderer;
import com.waylays.arcanes.client.render.GoldItemProjectileRenderer;
import com.waylays.arcanes.client.render.GoldSpikeRenderer;
import com.waylays.arcanes.client.render.GoldenSentinelRenderer;
import com.waylays.arcanes.client.render.ImplosionRenderer;
import com.waylays.arcanes.client.render.InkHandsRenderer;
import com.waylays.arcanes.client.render.InkWaveRenderer;
import com.waylays.arcanes.client.render.ManaCamelRenderer;
import com.waylays.arcanes.client.render.ManaCreatureRenderer;
import com.waylays.arcanes.client.render.ManaSalmonRenderer;
import com.waylays.arcanes.client.render.MissileRenderer;
import com.waylays.arcanes.client.render.MoltenPoolRenderer;
import com.waylays.arcanes.client.render.OrigamiDragonRenderer;
import com.waylays.arcanes.client.render.PageStormRenderer;
import com.waylays.arcanes.client.render.PaperCraneRenderer;
import com.waylays.arcanes.client.render.PaperTempestRenderer;
import com.waylays.arcanes.client.render.QuillStrokeRenderer;
import com.waylays.arcanes.client.render.RazorPageRenderer;
import com.waylays.arcanes.client.render.RewriteAuraRenderer;
import com.waylays.arcanes.client.render.RuinSigilRenderer;
import com.waylays.arcanes.client.render.ShieldBubbleRenderer;
import com.waylays.arcanes.client.render.SilenceSealRenderer;
import com.waylays.arcanes.client.render.StrikeTrailRenderer;
import com.waylays.arcanes.client.render.TomeOfJudgmentRenderer;
import com.waylays.arcanes.client.render.WordOfPowerRenderer;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModItems;
import io.redspace.ironsspellbooks.render.SpellBookCurioRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@EventBusSubscriber(modid = ArcanesOublies.MODID, value = Dist.CLIENT)
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

        event.registerEntityRenderer(ModEntities.BLAST_EFFECT.get(), BlastEffectRenderer::new);
        event.registerEntityRenderer(ModEntities.EXPLOSIVE_ORB.get(), context -> new GlowOrbRenderer<>(context, .8f, 0xFF7A2A));
        event.registerEntityRenderer(ModEntities.CHAIN_DETONATION.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.RUIN_SIGIL.get(), RuinSigilRenderer::new);
        event.registerEntityRenderer(ModEntities.DETONATION_WAVE.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.IMPLOSION_CORE.get(), ImplosionRenderer::new);
        event.registerEntityRenderer(ModEntities.CLUSTER_SHELL.get(), context -> new GlowOrbRenderer<>(context, 1.1f, 0xB040FF));
        event.registerEntityRenderer(ModEntities.CLUSTER_BOMBLET.get(), context -> new GlowOrbRenderer<>(context, .4f, 0xFF8A3A));
        event.registerEntityRenderer(ModEntities.SHIELD_BUBBLE.get(), ShieldBubbleRenderer::new);
        event.registerEntityRenderer(ModEntities.CATACLYSM_CORE.get(), CataclysmRenderer::new);

        event.registerEntityRenderer(ModEntities.PAGE_STORM.get(), PageStormRenderer::new);
        event.registerEntityRenderer(ModEntities.INK_WAVE.get(), InkWaveRenderer::new);
        event.registerEntityRenderer(ModEntities.INK_HANDS.get(), InkHandsRenderer::new);
        event.registerEntityRenderer(ModEntities.FINAL_CHAPTER.get(), FinalChapterRenderer::new);
        event.registerEntityRenderer(ModEntities.CHAPTER_RUNE.get(), ChapterRuneRenderer::new);
        event.registerEntityRenderer(ModEntities.RAZOR_PAGE.get(), RazorPageRenderer::new);
        event.registerEntityRenderer(ModEntities.TOME_OF_JUDGMENT.get(), TomeOfJudgmentRenderer::new);
        event.registerEntityRenderer(ModEntities.BOOKMARK_MARKER.get(), BookmarkRenderer::new);
        event.registerEntityRenderer(ModEntities.SILENCE_SEAL.get(), SilenceSealRenderer::new);
        event.registerEntityRenderer(ModEntities.FLYING_GRIMOIRE.get(), FlyingGrimoireRenderer::new);
        event.registerEntityRenderer(ModEntities.GRIMOIRE_BOLT.get(), context -> new GlowOrbRenderer<>(context, .4f, 0xA060FF));
        event.registerEntityRenderer(ModEntities.WORD_OF_POWER.get(), WordOfPowerRenderer::new);
        event.registerEntityRenderer(ModEntities.REWRITE_AURA.get(), RewriteAuraRenderer::new);
        event.registerEntityRenderer(ModEntities.PAPER_CRANE.get(), PaperCraneRenderer::new);
        event.registerEntityRenderer(ModEntities.QUILL_STROKE.get(), QuillStrokeRenderer::new);
        event.registerEntityRenderer(ModEntities.AEGIS_PAGES.get(), AegisPagesRenderer::new);
        event.registerEntityRenderer(ModEntities.PAPER_TEMPEST.get(), PaperTempestRenderer::new);
        event.registerEntityRenderer(ModEntities.FLYING_STACK.get(), FlyingStackRenderer::new);
        event.registerEntityRenderer(ModEntities.ORIGAMI_DRAGON.get(), OrigamiDragonRenderer::new);
        event.registerEntityRenderer(ModEntities.STRIKE_TRAIL.get(), StrikeTrailRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSILE.get(), MissileRenderer::new);

        event.registerEntityRenderer(ModEntities.MANA_RAT.get(),
                context -> new ManaCreatureRenderer<>(context, new ManaRatModel(context.bakeLayer(ManaRatModel.LAYER)), .25f, 1.2f, true));
        event.registerEntityRenderer(ModEntities.MANA_SERPENT.get(),
                context -> new ManaCreatureRenderer<>(context, new ManaSerpentModel(context.bakeLayer(ManaSerpentModel.LAYER)), .35f, 1.1f, true));
        event.registerEntityRenderer(ModEntities.MANA_CROW.get(),
                context -> new ManaCreatureRenderer<>(context, new ManaCrowModel(context.bakeLayer(ManaCrowModel.LAYER)), .25f, 1.25f, true));
        event.registerEntityRenderer(ModEntities.MANA_SALMON.get(), ManaSalmonRenderer::new);
        event.registerEntityRenderer(ModEntities.MANA_DROMEDARY.get(), ManaCamelRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ManaRatModel.LAYER, ManaRatModel::createBodyLayer);
        event.registerLayerDefinition(ManaSerpentModel.LAYER, ManaSerpentModel::createBodyLayer);
        event.registerLayerDefinition(ManaCrowModel.LAYER, ManaCrowModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Spellbooks hang on the player's hip like Iron's own
        event.enqueueWork(() -> {
            CuriosRendererRegistry.register(ModItems.GOLDEN_GRIMOIRE.get(), SpellBookCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.DETONATION_CODEX.get(), SpellBookCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.LIVING_GRIMOIRE.get(), SpellBookCurioRenderer::new);
        });
    }
}
