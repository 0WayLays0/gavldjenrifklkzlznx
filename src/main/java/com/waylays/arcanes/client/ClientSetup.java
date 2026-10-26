package com.waylays.arcanes.client;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.model.ManaCrowModel;
import com.waylays.arcanes.client.model.ManaWolfModel;
import com.waylays.arcanes.client.model.ManaKangarooModel;
import com.waylays.arcanes.client.model.ManaFoxModel;
import com.waylays.arcanes.client.model.ManaFalconModel;
import com.waylays.arcanes.client.model.ManaButterflyModel;
import com.waylays.arcanes.client.model.ManaPiranhaModel;
import com.waylays.arcanes.client.model.ManaRatModel;
import com.waylays.arcanes.client.model.ManaSerpentModel;
import com.waylays.arcanes.client.render.AegisPagesRenderer;
import com.waylays.arcanes.client.render.AnnihilationBeamRenderer;
import com.waylays.arcanes.client.render.AurumBladeRenderer;
import com.waylays.arcanes.client.render.BlastEffectRenderer;
import com.waylays.arcanes.client.render.BookmarkRenderer;
import com.waylays.arcanes.client.render.BossPropRenderer;
import com.waylays.arcanes.client.render.BossTelegraphRenderer;
import com.waylays.arcanes.client.render.CataclysmRenderer;
import com.waylays.arcanes.client.render.ChapterRuneRenderer;
import com.waylays.arcanes.client.render.EdgeGlyphsRenderer;
import com.waylays.arcanes.client.render.ElderCircleRenderer;
import com.waylays.arcanes.client.render.GlyphArcRenderer;
import com.waylays.arcanes.client.render.GlyphBoltRenderer;
import com.waylays.arcanes.client.render.GlyphMarkRenderer;
import com.waylays.arcanes.client.render.GlyphSealRenderer;
import com.waylays.arcanes.client.render.GlyphSignRenderer;
import com.waylays.arcanes.client.render.GlyphStormRenderer;
import com.waylays.arcanes.client.render.GlyphWaveRenderer;
import com.waylays.arcanes.client.render.GlyphZoneRenderer;
import com.waylays.arcanes.client.render.TokenAegisRenderer;
import com.waylays.arcanes.client.render.TokenBoltRenderer;
import com.waylays.arcanes.client.render.TokenFlipRenderer;
import com.waylays.arcanes.client.render.TokenHandRenderer;
import com.waylays.arcanes.client.render.TokenMarkerRenderer;
import com.waylays.arcanes.client.render.TokenMineRenderer;
import com.waylays.arcanes.client.render.TokenMoteRenderer;
import com.waylays.arcanes.client.render.TokenStormRenderer;
import com.waylays.arcanes.client.render.TokenWaveRenderer;
import com.waylays.arcanes.client.render.BrickBoltRenderer;
import com.waylays.arcanes.client.render.BrickStormRenderer;
import com.waylays.arcanes.client.render.MeteorRenderer;
import com.waylays.arcanes.client.render.RitualCircleRenderer;
import com.waylays.arcanes.client.render.RitualDomeRenderer;
import com.waylays.arcanes.client.render.RitualPortalRenderer;
import com.waylays.arcanes.client.render.StormCloudRenderer;
import com.waylays.arcanes.client.render.CitadelRenderer;
import com.waylays.arcanes.client.render.GemSigilRenderer;
import com.waylays.arcanes.client.render.GemShatterRenderer;
import com.waylays.arcanes.client.render.RubyRayRenderer;
import com.waylays.arcanes.client.render.RubyHeartRenderer;
import com.waylays.arcanes.client.render.GemShardRenderer;
import com.waylays.arcanes.client.render.DiamondPrismRenderer;
import com.waylays.arcanes.client.render.DiamondShellRenderer;
import com.waylays.arcanes.client.render.LapisSealRenderer;
import com.waylays.arcanes.client.render.LapisConstellationRenderer;
import com.waylays.arcanes.client.render.AmethystResonanceRenderer;
import com.waylays.arcanes.client.render.AmethystGeodeRenderer;
import com.waylays.arcanes.client.render.EmeraldBloomRenderer;
import com.waylays.arcanes.client.render.GemCrownRenderer;
import com.waylays.arcanes.client.render.GoldBoltRenderer;
import com.waylays.arcanes.client.render.GoldCoinsRenderer;
import com.waylays.arcanes.client.render.GoldSigilRenderer;
import com.waylays.arcanes.client.render.RampartRenderer;
import com.waylays.arcanes.client.render.WallImpactRenderer;
import com.waylays.arcanes.client.render.WallPieceRenderer;
import com.waylays.arcanes.client.render.WallSigilRenderer;
import com.waylays.arcanes.client.render.AncientBellRenderer;
import com.waylays.arcanes.client.render.AncientPillarRenderer;
import com.waylays.arcanes.client.render.CopperSpikesRenderer;
import com.waylays.arcanes.client.render.RustBoltRenderer;
import com.waylays.arcanes.client.render.SporePatchRenderer;
import com.waylays.arcanes.client.render.SunkenTempleRenderer;
import com.waylays.arcanes.client.render.ToppleWallRenderer;
import com.waylays.arcanes.client.render.VineSnareRenderer;
import com.waylays.arcanes.client.render.DragonSunRenderer;
import com.waylays.arcanes.client.render.EldoradoSunRenderer;
import com.waylays.arcanes.client.render.FinalChapterRenderer;
import com.waylays.arcanes.client.render.FlyingGrimoireRenderer;
import com.waylays.arcanes.client.render.FlyingStackRenderer;
import com.waylays.arcanes.client.render.GildedChainsRenderer;
import com.waylays.arcanes.client.render.GlowOrbRenderer;
import com.waylays.arcanes.client.render.GoldenDragonRenderer;
import com.waylays.arcanes.client.render.GoldfallIngotRenderer;
import com.waylays.arcanes.client.render.MoltenGoldBlobRenderer;
import com.waylays.arcanes.client.render.GoldSpikeRenderer;
import com.waylays.arcanes.client.render.GoldenSentinelRenderer;
import com.waylays.arcanes.client.render.ImplosionRenderer;
import com.waylays.arcanes.client.render.InkHandsRenderer;
import com.waylays.arcanes.client.render.InkWaveRenderer;
import com.waylays.arcanes.client.render.ManaCamelRenderer;
import com.waylays.arcanes.client.render.ManaCreatureRenderer;
import com.waylays.arcanes.client.render.ManaPiranhaRenderer;
import com.waylays.arcanes.client.render.MissileRenderer;
import com.waylays.arcanes.client.render.MoltenPoolRenderer;
import com.waylays.arcanes.client.render.OrigamiDragonRenderer;
import com.waylays.arcanes.client.render.PageStormRenderer;
import com.waylays.arcanes.client.render.PaperCraneRenderer;
import com.waylays.arcanes.client.render.PaperTempestRenderer;
import com.waylays.arcanes.client.render.PetrifyBindRenderer;
import com.waylays.arcanes.client.render.QuillStrokeRenderer;
import com.waylays.arcanes.client.render.RazorPageRenderer;
import com.waylays.arcanes.client.render.RewriteAuraRenderer;
import com.waylays.arcanes.client.render.RuinSigilRenderer;
import com.waylays.arcanes.client.render.RuneBulwarkRenderer;
import com.waylays.arcanes.client.render.RuneShardRenderer;
import com.waylays.arcanes.client.render.RuneSigilRenderer;
import com.waylays.arcanes.client.render.RuneWallRenderer;
import com.waylays.arcanes.client.render.RunestoneRenderer;
import com.waylays.arcanes.client.render.ShieldBubbleRenderer;
import com.waylays.arcanes.client.render.SilenceSealRenderer;
import com.waylays.arcanes.client.render.StrikeTrailRenderer;
import com.waylays.arcanes.client.render.TomeOfJudgmentRenderer;
import com.waylays.arcanes.client.render.VoidOrbRenderer;
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
import com.waylays.arcanes.client.gold.GoldCoatLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@EventBusSubscriber(modid = ArcanesOublies.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GOLD_BOLT.get(), GoldBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLDFALL_INGOT.get(), GoldfallIngotRenderer::new);
        event.registerEntityRenderer(ModEntities.MOLTEN_GOLD_BLOB.get(), MoltenGoldBlobRenderer::new);
        event.registerEntityRenderer(ModEntities.GILDED_CHAINS.get(), GildedChainsRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLD_SPIKE.get(), GoldSpikeRenderer::new);
        event.registerEntityRenderer(ModEntities.AURUM_BLADE.get(), AurumBladeRenderer::new);
        event.registerEntityRenderer(ModEntities.MOLTEN_POOL.get(), MoltenPoolRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLDEN_SENTINEL.get(), GoldenSentinelRenderer::new);
        event.registerEntityRenderer(ModEntities.ELDORADO_SUN.get(), EldoradoSunRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLDEN_DRAGON.get(), GoldenDragonRenderer::new);
        event.registerEntityRenderer(ModEntities.DRAGON_SUN.get(), DragonSunRenderer::new);

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

        event.registerEntityRenderer(ModEntities.BOSS_TELEGRAPH.get(), BossTelegraphRenderer::new);
        event.registerEntityRenderer(ModEntities.BOSS_PROP.get(), BossPropRenderer::new);
        event.registerEntityRenderer(ModEntities.ANNIHILATION_BEAM.get(), AnnihilationBeamRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_ORB.get(), VoidOrbRenderer::new);
        event.registerEntityRenderer(ModEntities.RUNE_WALL.get(), RuneWallRenderer::new);

        event.registerEntityRenderer(ModEntities.RUNE_SIGIL.get(), RuneSigilRenderer::new);
        event.registerEntityRenderer(ModEntities.RUNESTONE.get(), RunestoneRenderer::new);
        event.registerEntityRenderer(ModEntities.RUNE_BULWARK.get(), RuneBulwarkRenderer::new);
        event.registerEntityRenderer(ModEntities.RUNE_SHARD.get(), RuneShardRenderer::new);
        event.registerEntityRenderer(ModEntities.PETRIFY_BIND.get(), PetrifyBindRenderer::new);
        event.registerEntityRenderer(ModEntities.EDGE_GLYPHS.get(), EdgeGlyphsRenderer::new);
        event.registerEntityRenderer(ModEntities.ELDER_CIRCLE.get(), ElderCircleRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPH_SIGN.get(), GlyphSignRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPH_BOLT.get(), GlyphBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPH_WAVE.get(), GlyphWaveRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPH_ARC.get(), GlyphArcRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPH_MARK.get(), GlyphMarkRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPH_ZONE.get(), GlyphZoneRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPH_SEAL.get(), GlyphSealRenderer::new);
        event.registerEntityRenderer(ModEntities.GLYPH_STORM.get(), GlyphStormRenderer::new);
        event.registerEntityRenderer(ModEntities.TOKEN_BOLT.get(), TokenBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.TOKEN_MARKER.get(), TokenMarkerRenderer::new);
        event.registerEntityRenderer(ModEntities.TOKEN_MINE.get(), TokenMineRenderer::new);
        event.registerEntityRenderer(ModEntities.TOKEN_MOTE.get(), TokenMoteRenderer::new);
        event.registerEntityRenderer(ModEntities.TOKEN_FLIP.get(), TokenFlipRenderer::new);
        event.registerEntityRenderer(ModEntities.TOKEN_AEGIS.get(), TokenAegisRenderer::new);
        event.registerEntityRenderer(ModEntities.TOKEN_STORM.get(), TokenStormRenderer::new);
        event.registerEntityRenderer(ModEntities.TOKEN_WAVE.get(), TokenWaveRenderer::new);
        event.registerEntityRenderer(ModEntities.TOKEN_HAND.get(), TokenHandRenderer::new);
        event.registerEntityRenderer(ModEntities.ANCIENT_PILLAR.get(), AncientPillarRenderer::new);
        event.registerEntityRenderer(ModEntities.COPPER_SPIKES.get(), CopperSpikesRenderer::new);
        event.registerEntityRenderer(ModEntities.TOPPLE_WALL.get(), ToppleWallRenderer::new);
        event.registerEntityRenderer(ModEntities.ANCIENT_BELL.get(), AncientBellRenderer::new);
        event.registerEntityRenderer(ModEntities.VINE_SNARE.get(), VineSnareRenderer::new);
        event.registerEntityRenderer(ModEntities.SPORE_PATCH.get(), SporePatchRenderer::new);
        event.registerEntityRenderer(ModEntities.SUNKEN_TEMPLE.get(), SunkenTempleRenderer::new);
        event.registerEntityRenderer(ModEntities.RUST_BOLT.get(), RustBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.BRICK_BOLT.get(), BrickBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.WALL_PIECE.get(), WallPieceRenderer::new);
        event.registerEntityRenderer(ModEntities.CITADEL.get(), CitadelRenderer::new);
        event.registerEntityRenderer(ModEntities.RAMPART.get(), RampartRenderer::new);
        event.registerEntityRenderer(ModEntities.BRICK_STORM.get(), BrickStormRenderer::new);
        event.registerEntityRenderer(ModEntities.WALL_SIGIL.get(), WallSigilRenderer::new);
        event.registerEntityRenderer(ModEntities.WALL_IMPACT.get(), WallImpactRenderer::new);
        event.registerEntityRenderer(ModEntities.GEM_SIGIL.get(), GemSigilRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLD_SIGIL.get(), GoldSigilRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLD_COINS.get(), GoldCoinsRenderer::new);
        event.registerEntityRenderer(ModEntities.GEM_SHATTER.get(), GemShatterRenderer::new);
        event.registerEntityRenderer(ModEntities.RUBY_RAY.get(), RubyRayRenderer::new);
        event.registerEntityRenderer(ModEntities.RUBY_HEART.get(), RubyHeartRenderer::new);
        event.registerEntityRenderer(ModEntities.GEM_SHARD.get(), GemShardRenderer::new);
        event.registerEntityRenderer(ModEntities.DIAMOND_PRISM.get(), DiamondPrismRenderer::new);
        event.registerEntityRenderer(ModEntities.DIAMOND_SHELL.get(), DiamondShellRenderer::new);
        event.registerEntityRenderer(ModEntities.LAPIS_SEAL.get(), LapisSealRenderer::new);
        event.registerEntityRenderer(ModEntities.LAPIS_CONSTELLATION.get(), LapisConstellationRenderer::new);
        event.registerEntityRenderer(ModEntities.AMETHYST_RESONANCE.get(), AmethystResonanceRenderer::new);
        event.registerEntityRenderer(ModEntities.AMETHYST_GEODE.get(), AmethystGeodeRenderer::new);
        event.registerEntityRenderer(ModEntities.EMERALD_BLOOM.get(), EmeraldBloomRenderer::new);
        event.registerEntityRenderer(ModEntities.GEM_CROWN.get(), GemCrownRenderer::new);
        event.registerEntityRenderer(ModEntities.TIME_DOOR.get(), com.waylays.arcanes.client.render.TimeDoorRenderer::new);
        event.registerEntityRenderer(ModEntities.TEMPORAL_ECHO.get(), com.waylays.arcanes.client.render.TemporalEchoRenderer::new);
        event.registerEntityRenderer(ModEntities.PRUNING_BATON.get(), com.waylays.arcanes.client.render.PruningBatonRenderer::new);
        event.registerEntityRenderer(ModEntities.TIME_LOOP.get(), com.waylays.arcanes.client.render.TimeLoopRenderer::new);
        event.registerEntityRenderer(ModEntities.COSMIC_VARIANT.get(), com.waylays.arcanes.client.render.CosmicVariantRenderer::new);
        event.registerEntityRenderer(ModEntities.TIME_STOP_BUBBLE.get(), com.waylays.arcanes.client.render.TimeStopBubbleRenderer::new);
        event.registerEntityRenderer(ModEntities.RITUAL_CIRCLE.get(), RitualCircleRenderer::new);
        event.registerEntityRenderer(ModEntities.METEOR.get(), MeteorRenderer::new);
        event.registerEntityRenderer(ModEntities.RITUAL_DOME.get(), RitualDomeRenderer::new);
        event.registerEntityRenderer(ModEntities.STORM_CLOUD.get(), StormCloudRenderer::new);
        event.registerEntityRenderer(ModEntities.RITUAL_PORTAL.get(), RitualPortalRenderer::new);

        event.registerEntityRenderer(ModEntities.MANA_RAT.get(),
                context -> new ManaCreatureRenderer<>(context, new ManaRatModel(context.bakeLayer(ManaRatModel.LAYER)), .25f, 1.2f, true));
        event.registerEntityRenderer(ModEntities.MANA_SERPENT.get(),
                context -> new ManaCreatureRenderer<>(context, new ManaSerpentModel(context.bakeLayer(ManaSerpentModel.LAYER)), .35f, 1.1f, true));
        event.registerEntityRenderer(ModEntities.MANA_CROW.get(),
                context -> new ManaCreatureRenderer<>(context, new ManaCrowModel(context.bakeLayer(ManaCrowModel.LAYER)), .25f, 1.25f, true));
        event.registerEntityRenderer(ModEntities.MANA_PIRANHA.get(), ManaPiranhaRenderer::new);
        event.registerEntityRenderer(ModEntities.MANA_DROMEDARY.get(), ManaCamelRenderer::new);
        event.registerEntityRenderer(ModEntities.MANA_FOX.get(),
                context -> new ManaCreatureRenderer<>(context, new ManaFoxModel(context.bakeLayer(ManaFoxModel.LAYER)), .3f, 1.05f, true));
        event.registerEntityRenderer(ModEntities.MANA_WOLF.get(),
                context -> new ManaCreatureRenderer<>(context, new ManaWolfModel(context.bakeLayer(ManaWolfModel.LAYER)), .4f, 1.15f, true));
        event.registerEntityRenderer(ModEntities.MANA_BUTTERFLY.get(),
                context -> new ManaCreatureRenderer<>(context, new ManaButterflyModel(context.bakeLayer(ManaButterflyModel.LAYER)), .15f, 1.1f, true));
        event.registerEntityRenderer(ModEntities.MANA_KANGAROO.get(),
                context -> new ManaCreatureRenderer<>(context, new ManaKangarooModel(context.bakeLayer(ManaKangarooModel.LAYER)), .4f, 1.05f, true));
        event.registerEntityRenderer(ModEntities.MANA_FALCON.get(),
                context -> new ManaCreatureRenderer<>(context, new ManaFalconModel(context.bakeLayer(ManaFalconModel.LAYER)), .6f, 1.5f, true));
    }

    /** The gold of Gold magic can cover any body: every living thing's renderer, and the players', draw it. */
    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addGoldCoat(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : event.getEntityTypes()) {
            if (event.getRenderer((EntityType) type) instanceof LivingEntityRenderer renderer) {
                renderer.addLayer(new GoldCoatLayer(renderer));
            }
        }
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof LivingEntityRenderer renderer) {
                renderer.addLayer(new GoldCoatLayer(renderer));
            }
        }
    }

    /** The sky of the Pocket Universe (the dimension type's effects). */
    @SubscribeEvent
    public static void registerSkies(net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
        event.register(ArcanesOublies.id("pocket"), new com.waylays.arcanes.client.render.PocketSkyEffects());
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ManaRatModel.LAYER, ManaRatModel::createBodyLayer);
        event.registerLayerDefinition(ManaSerpentModel.LAYER, ManaSerpentModel::createBodyLayer);
        event.registerLayerDefinition(ManaCrowModel.LAYER, ManaCrowModel::createBodyLayer);
        event.registerLayerDefinition(ManaPiranhaModel.LAYER, ManaPiranhaModel::createBodyLayer);
        event.registerLayerDefinition(ManaFoxModel.LAYER, ManaFoxModel::createBodyLayer);
        event.registerLayerDefinition(ManaWolfModel.LAYER, ManaWolfModel::createBodyLayer);
        event.registerLayerDefinition(ManaButterflyModel.LAYER, ManaButterflyModel::createBodyLayer);
        event.registerLayerDefinition(ManaKangarooModel.LAYER, ManaKangarooModel::createBodyLayer);
        event.registerLayerDefinition(ManaFalconModel.LAYER, ManaFalconModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Spellbooks hang on the player's hip like Iron's own
        event.enqueueWork(() -> {
            CuriosRendererRegistry.register(ModItems.GOLDEN_GRIMOIRE.get(), SpellBookCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.DETONATION_CODEX.get(), SpellBookCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.LIVING_GRIMOIRE.get(), SpellBookCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.TYRANTS_CODEX.get(), SpellBookCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.ELDER_RUNE_CODEX.get(), SpellBookCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.SIX_INKS_CODEX.get(), SpellBookCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.SOUL_LEDGER.get(), SpellBookCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.RUSTED_CODEX.get(), SpellBookCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.RAMPART_CODEX.get(), SpellBookCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.JEWELLERS_CODEX.get(), SpellBookCurioRenderer::new);
        });
    }
}
