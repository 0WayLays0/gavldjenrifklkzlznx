package com.waylays.cinematic;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import com.waylays.cinematic.client.ActorRenderer;
import com.waylays.cinematic.client.CameraTrack;
import com.waylays.cinematic.client.ClientStudio;
import com.waylays.cinematic.client.DirectorScreen;
import com.waylays.cinematic.client.EmoteScreen;
import com.waylays.cinematic.client.SkinCache;
import com.waylays.cinematic.client.StudioCamera;
import com.waylays.cinematic.client.StudioHud;
import com.waylays.cinematic.client.StudioScreen;
import com.waylays.cinematic.studio.StudioRegistry;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

/** Wires keys and events to the film look ({@link FilmLook}) and the cameras ({@link CameraDirector}). */
public final class CinematicClient {
    private static final String CATEGORY = "key.categories.cinematic";
    static final KeyMapping TOGGLE_LOOK = key("key.cinematic.toggle_look", GLFW.GLFW_KEY_F7);
    static final KeyMapping TOGGLE_CAMERA = key("key.cinematic.toggle_camera", GLFW.GLFW_KEY_F8);
    static final KeyMapping NEXT_SHOT = key("key.cinematic.next_shot", GLFW.GLFW_KEY_F9);
    static final KeyMapping TOGGLE_DOF = key("key.cinematic.toggle_dof", GLFW.GLFW_KEY_F10);
    static final KeyMapping STUDIO = key("key.cinematic.studio", GLFW.GLFW_KEY_F6);
    static final KeyMapping FREECAM = key("key.cinematic.freecam", GLFW.GLFW_KEY_N);
    static final KeyMapping CAM_POINT = key("key.cinematic.camera_point", GLFW.GLFW_KEY_K);
    static final KeyMapping EMOTES = key("key.cinematic.emotes", GLFW.GLFW_KEY_B);
    static final KeyMapping PLAY = key("key.cinematic.play", GLFW.GLFW_KEY_P);
    static final KeyMapping BACK = key("key.cinematic.back", GLFW.GLFW_KEY_LEFT);
    static final KeyMapping FORWARD = key("key.cinematic.forward", GLFW.GLFW_KEY_RIGHT);

    private static boolean lookOn = true;
    private static boolean dofOn = true;

    private CinematicClient() {}

    private static KeyMapping key(String name, int glfwKey) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, glfwKey, CATEGORY);
    }

    static void init() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(CinematicClient::onKeys);
        modBus.addListener(CinematicClient::onReloadListeners);
        modBus.addListener(CinematicClient::onSetup);
        modBus.addListener(CinematicClient::onRenderers);
        modBus.addListener(CinematicClient::onLayers);
        ClientStudio.init();
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(CinematicClient::onClientTick);
        bus.addListener(CinematicClient::onRenderStage);
        bus.addListener(CinematicClient::onCameraAngles);
        bus.addListener(CinematicClient::onFov);
        bus.addListener(CinematicClient::onLogin);
        bus.addListener(CinematicClient::onLogout);
        bus.addListener(StudioCamera::onMovementInput);
        bus.addListener(StudioCamera::onScroll);
        bus.addListener(StudioCamera::onInteract);
        bus.addListener(StudioHud::onGui);
        bus.addListener(StudioHud::onGuiOverlay);
        bus.addListener(StudioHud::onLevel);
        bus.addListener(CinematicClient::onRenderPlayer);
    }

    private static void onSetup(FMLClientSetupEvent event) {
        lookOn = CinematicConfig.LOOK_ON_AT_START.get();
        dofOn = CinematicConfig.DEPTH_OF_FIELD.get();
    }

    private static void onKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_LOOK);
        event.register(TOGGLE_CAMERA);
        event.register(NEXT_SHOT);
        event.register(TOGGLE_DOF);
        for (KeyMapping k : new KeyMapping[] {STUDIO, FREECAM, CAM_POINT, EMOTES, PLAY, BACK, FORWARD}) event.register(k);
    }

    private static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.waylays.cinematic.client.ActorLayers.WIDE, () -> com.waylays.cinematic.client.ActorLayers.create(false));
        event.registerLayerDefinition(com.waylays.cinematic.client.ActorLayers.SLIM, () -> com.waylays.cinematic.client.ActorLayers.create(true));
    }

    private static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(StudioRegistry.ACTOR.get(), ActorRenderer::new);
    }

    /** The director is not in their own film: hidden while a studio camera looks around. */
    private static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (StudioCamera.hideSelf && StudioCamera.anyActive() && event.getEntity() == Minecraft.getInstance().player) event.setCanceled(true);
    }

    private static void onReloadListeners(RegisterClientReloadListenersEvent event) {
        // Shaders come from resource packs: rebuild them after F3+T or a pack change
        event.registerReloadListener((ResourceManagerReloadListener) manager -> FilmLook.close());
    }

    public static boolean lookOn() { return lookOn; }
    public static boolean dofOn() { return dofOn; }

    private static void say(String key, boolean on) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable(key,
                    Component.translatable(on ? "cinematic.state.on" : "cinematic.state.off")), true);
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (TOGGLE_LOOK.consumeClick()) {
            lookOn = !lookOn;
            say("cinematic.msg.look", lookOn);
        }
        while (TOGGLE_DOF.consumeClick()) {
            dofOn = !dofOn;
            say("cinematic.msg.dof", dofOn);
        }
        while (TOGGLE_CAMERA.consumeClick()) {
            if (mc.player != null) {
                CameraDirector.toggle(mc);
                say("cinematic.msg.camera", CameraDirector.active());
            }
        }
        while (NEXT_SHOT.consumeClick()) {
            if (CameraDirector.active()) CameraDirector.nextShot(mc, true);
        }
        while (STUDIO.consumeClick()) {
            if (mc.player != null) mc.setScreen(new StudioScreen());
        }
        while (FREECAM.consumeClick()) StudioCamera.toggleFree(mc);
        while (EMOTES.consumeClick()) {
            if (mc.player != null) mc.setScreen(new EmoteScreen());
        }
        while (CAM_POINT.consumeClick()) {
            if (mc.player != null && !ClientStudio.selectedScene.isEmpty()) {
                mc.player.displayClientMessage(DirectorScreen.addPoint(CameraTrack.forScene(ClientStudio.selectedScene)), true);
            } else if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable("cinematic.studio.make_scene"), true);
            }
        }
        while (PLAY.consumeClick()) {
            if (mc.player == null) continue;
            if (ClientStudio.isRecording() || ClientStudio.serverPlaying() || ClientStudio.play().countdown() > 0) {
                ClientStudio.stopOnServer();
            } else if (ClientStudio.canDirect() && ClientStudio.serverHasMod() && !ClientStudio.selectedScene.isEmpty()) {
                ClientStudio.playOnServer();
            } else {
                ClientStudio.previewing = !ClientStudio.previewing;
            }
        }
        int step = net.minecraft.client.gui.screens.Screen.hasShiftDown() ? 20 : (net.minecraft.client.gui.screens.Screen.hasControlDown() ? 1 : 5);
        while (BACK.consumeClick()) scrub(-step);
        while (FORWARD.consumeClick()) scrub(step);
        ClientStudio.tick(mc);
        StudioCamera.tick(mc);
        CameraDirector.tick(mc);
    }

    private static void scrub(int ticks) {
        if (ClientStudio.serverPlaying()) return;
        ClientStudio.previewing = false;
        ClientStudio.cursor = Math.max(0, Math.min(ClientStudio.sceneLength(ClientStudio.selectedScene), Math.round(ClientStudio.cursor) + ticks));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(com.waylays.cinematic.client.TimelineSlider.time(ClientStudio.cursor)), true);
        }
    }

    private static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            FilmLook.render(event.getPartialTick());
        }
    }

    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (StudioCamera.onCameraAngles(event)) return;
        CameraDirector.onCameraAngles(event);
        StudioCamera.noteView(event.getCamera(), Minecraft.getInstance().options.fov().get());
    }

    private static void onFov(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) return;
        if (StudioCamera.onFov(event)) return;
        CameraDirector.onFov(event);
    }

    private static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (CinematicConfig.INTRO_FADE.get()) FilmLook.fadeFromBlack(2.8f);
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft mc = Minecraft.getInstance();
        CameraDirector.stop(mc);
        StudioCamera.stopAll(mc);
        ClientStudio.reset();
        SkinCache.clearRequests();
        CameraTrack.forgetLoaded();
    }
}
