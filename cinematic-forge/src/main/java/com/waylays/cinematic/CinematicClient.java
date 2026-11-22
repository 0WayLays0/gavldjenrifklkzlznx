package com.waylays.cinematic;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
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
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(CinematicClient::onClientTick);
        bus.addListener(CinematicClient::onRenderStage);
        bus.addListener(CinematicClient::onCameraAngles);
        bus.addListener(CinematicClient::onFov);
        bus.addListener(CinematicClient::onLogin);
        bus.addListener(CinematicClient::onLogout);
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
        CameraDirector.tick(mc);
    }

    private static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            FilmLook.render(event.getPartialTick());
        }
    }

    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CameraDirector.onCameraAngles(event);
    }

    private static void onFov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov()) CameraDirector.onFov(event);
    }

    private static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (CinematicConfig.INTRO_FADE.get()) FilmLook.fadeFromBlack(2.8f);
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        CameraDirector.stop(Minecraft.getInstance());
    }
}
