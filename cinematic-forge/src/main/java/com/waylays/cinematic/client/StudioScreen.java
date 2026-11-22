package com.waylays.cinematic.client;

import com.waylays.cinematic.net.ActionMsg.Action;
import com.waylays.cinematic.net.SyncMsg;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** The studio: scenes on the left, the scene's actors on the right (click one to dress and direct it), playback below. */
public class StudioScreen extends Screen {
    private static int listOffset;

    private EditBox sceneBox, actorBox;
    private TimelineSlider timeline;
    private boolean confirmDelete;

    public StudioScreen() {
        super(Component.translatable("cinematic.studio.title"));
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    private Button btn(Component label, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(label, b -> action.run()).bounds(x, y, w, 20).build());
    }

    void refresh() {
        String s = sceneBox == null ? "" : sceneBox.getValue();
        String a = actorBox == null ? "" : actorBox.getValue();
        rebuildWidgets();
        sceneBox.setValue(s);
        actorBox.setValue(a);
    }

    @Override
    protected void init() {
        SyncMsg.SceneInfo scene = ClientStudio.scene(ClientStudio.selectedScene);
        boolean can = ClientStudio.canDirect() && ClientStudio.serverHasMod();
        int left = 8, lw = 150, top = 22;

        // Scenes
        btn(Component.literal("<"), left, top, 20, () -> cycleScene(-1));
        btn(Component.literal(">"), left + lw - 20, top, 20, () -> cycleScene(1));
        sceneBox = addRenderableWidget(new EditBox(font, left + 1, top + 25, lw - 56, 18, tr("cinematic.studio.new_scene")));
        sceneBox.setMaxLength(32);
        sceneBox.setHint(tr("cinematic.studio.new_scene"));
        btn(tr("cinematic.studio.add"), left + lw - 52, top + 24, 52, () -> {
            if (sceneBox.getValue().isBlank()) return;
            ClientStudio.selectedScene = sceneBox.getValue().trim();
            ClientStudio.cursor = 0;
            ClientStudio.send(Action.CREATE_SCENE, null, sceneBox.getValue().trim(), 0);
            sceneBox.setValue("");
        }).active = can;
        btn(confirmDelete ? tr("cinematic.studio.confirm") : tr("cinematic.studio.delete_scene"), left, top + 48, lw, () -> {
            if (!confirmDelete) {
                confirmDelete = true;
                refresh();
                return;
            }
            confirmDelete = false;
            ClientStudio.send(Action.DELETE_SCENE, null, "", 0);
        }).active = can && scene != null;

        // Actors
        int rx = left + lw + 8, rw = width - rx - 8;
        int rows = Math.max(1, (height - top - 60) / 22 - 1);
        List<SyncMsg.ActorInfo> actors = scene == null ? List.of() : scene.actors();
        listOffset = Math.max(0, Math.min(listOffset, actors.size() - rows));
        boolean scroll = actors.size() > rows;
        int bw = scroll ? rw - 22 : rw;
        for (int i = 0; i < rows && listOffset + i < actors.size(); i++) {
            SyncMsg.ActorInfo a = actors.get(listOffset + i);
            btn(Component.literal(a.name() + "   " + TimelineSlider.time(a.length())), rx, top + i * 22, bw,
                    () -> minecraft.setScreen(new ActorScreen(a.id())));
        }
        if (scroll) {
            btn(Component.literal("^"), rx + rw - 20, top, 20, () -> {
                listOffset = Math.max(0, listOffset - 1);
                refresh();
            });
            btn(Component.literal("v"), rx + rw - 20, top + (rows - 1) * 22, 20, () -> {
                listOffset++;
                refresh();
            });
        }
        int ny = top + rows * 22;
        actorBox = addRenderableWidget(new EditBox(font, rx + 1, ny + 1, rw - 66, 18, tr("cinematic.studio.new_actor")));
        actorBox.setMaxLength(32);
        actorBox.setHint(tr("cinematic.studio.new_actor"));
        btn(tr("cinematic.studio.add_actor"), rx + rw - 62, ny, 62, () -> {
            ClientStudio.send(Action.CREATE_ACTOR, null, actorBox.getValue().trim(), 0);
            actorBox.setValue("");
        }).active = can && scene != null;

        // Playback
        timeline = addRenderableWidget(new TimelineSlider(8, height - 50, width - 16, 20, () -> ClientStudio.sceneLength(ClientStudio.selectedScene)));
        int n = 5, w = Math.min(120, (width - 16 - (n - 1) * 3) / n), by = height - 26, x = 8;
        btn(tr("cinematic.studio.play"), x, by, w, () -> {
            ClientStudio.playOnServer();
            onClose();
        }).active = can && scene != null && !ClientStudio.isRecording();
        x += w + 3;
        btn(tr("cinematic.studio.stop"), x, by, w, ClientStudio::stopOnServer).active = can;
        x += w + 3;
        btn(tr(ClientStudio.previewing ? "cinematic.studio.preview_pause" : "cinematic.studio.preview"), x, by, w, () -> {
            if (!ClientStudio.serverPlaying()) {
                if (!ClientStudio.previewing && ClientStudio.cursor >= ClientStudio.sceneLength(ClientStudio.selectedScene)) ClientStudio.cursor = 0;
                ClientStudio.previewing = !ClientStudio.previewing;
            }
            refresh();
        });
        x += w + 3;
        btn(tr("cinematic.studio.director"), x, by, w, () -> minecraft.setScreen(new DirectorScreen()));
        x += w + 3;
        btn(tr("cinematic.studio.emotes"), x, by, w, () -> minecraft.setScreen(new EmoteScreen()));
    }

    private void cycleScene(int dir) {
        List<SyncMsg.SceneInfo> scenes = ClientStudio.scenes();
        if (scenes.isEmpty()) return;
        int i = 0;
        for (int k = 0; k < scenes.size(); k++) if (scenes.get(k).name().equals(ClientStudio.selectedScene)) i = k;
        i = Math.floorMod(i + dir, scenes.size());
        ClientStudio.selectedScene = scenes.get(i).name();
        ClientStudio.cursor = 0;
        ClientStudio.previewing = false;
        listOffset = 0;
        confirmDelete = false;
        refresh();
    }

    @Override
    public void tick() {
        sceneBox.tick();
        actorBox.tick();
        if (!timeline.isFocused() || ClientStudio.serverPlaying() || ClientStudio.previewing) timeline.sync();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x90000000);
        g.drawString(font, title, 8, 8, 0xFFFFFF);
        g.drawString(font, tr("cinematic.studio.actors"), 166, 8, 0xC0C0C0);
        String sceneName = ClientStudio.selectedScene.isEmpty() ? "-" : ClientStudio.selectedScene;
        g.drawCenteredString(font, Component.literal(sceneName), 8 + 75, 28, 0xFFE070);
        int y = 96;
        if (!ClientStudio.serverHasMod()) {
            g.drawWordWrap(font, tr("cinematic.studio.no_server_mod"), 8, y, 150, 0xFF7070);
        } else if (!ClientStudio.canDirect()) {
            g.drawWordWrap(font, tr("cinematic.studio.no_permission"), 8, y, 150, 0xFF9090);
        } else if (ClientStudio.scene(ClientStudio.selectedScene) == null) {
            g.drawWordWrap(font, tr("cinematic.studio.make_scene"), 8, y, 150, 0xA0A0A0);
        } else {
            g.drawWordWrap(font, tr("cinematic.studio.pick_actor"), 8, y, 150, 0xA0A0A0);
        }
        if (ClientStudio.serverPlaying()) {
            g.drawString(font, tr("cinematic.studio.playing", ClientStudio.play().scene()), 8, height - 64, 0x80FF80);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
