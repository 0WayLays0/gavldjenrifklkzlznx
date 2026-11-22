package com.waylays.cinematic.client;

import com.waylays.cinematic.net.ActionMsg.Action;
import com.waylays.cinematic.net.SyncMsg;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** The studio: the scene on the left, its actors on the right (click one to dress and direct it), playback below. */
public class StudioScreen extends Screen {
    private static int listOffset;

    private EditBox sceneBox, actorBox;
    private TimelineSlider timeline;
    private boolean confirmDelete;
    // Layout, computed in init and reused in render
    private int lw, rx, rw, bottomTop, helpY;

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
        int m = Ui.MARGIN, p = Ui.pitch(height);
        lw = Math.min(210, (width - 3 * m) * 2 / 5);
        rx = m + lw + 18;
        rw = width - rx - m;
        bottomTop = height - 76;
        int y = Ui.TOP + 15;

        // The scene
        btn(Component.literal("<"), m, y, 20, () -> cycleScene(-1));
        btn(Component.literal(">"), m + lw - 20, y, 20, () -> cycleScene(1));
        y += p;
        sceneBox = addRenderableWidget(new EditBox(font, m + 1, y + 1, lw - 70, 18, tr("cinematic.studio.new_scene")));
        sceneBox.setMaxLength(32);
        sceneBox.setHint(tr("cinematic.studio.new_scene"));
        btn(tr("cinematic.studio.add"), m + lw - 64, y, 64, () -> {
            if (sceneBox.getValue().isBlank()) return;
            ClientStudio.selectedScene = sceneBox.getValue().trim();
            ClientStudio.cursor = 0;
            ClientStudio.send(Action.CREATE_SCENE, null, sceneBox.getValue().trim(), 0);
            sceneBox.setValue("");
        }).active = can;
        y += p;
        btn(confirmDelete ? tr("cinematic.studio.confirm") : tr("cinematic.studio.delete_scene"), m, y, lw, () -> {
            if (!confirmDelete) {
                confirmDelete = true;
                refresh();
                return;
            }
            confirmDelete = false;
            ClientStudio.send(Action.DELETE_SCENE, null, "", 0);
        }).active = can && scene != null;
        helpY = y + p + 4;

        // The actors
        int listTop = Ui.TOP + 15, listBottom = bottomTop - 18;
        int rows = Math.max(1, (listBottom - listTop) / p - 1);
        List<SyncMsg.ActorInfo> actors = scene == null ? List.of() : scene.actors();
        listOffset = Math.max(0, Math.min(listOffset, actors.size() - rows));
        boolean scroll = actors.size() > rows;
        int bw = scroll ? rw - 26 : rw;
        for (int i = 0; i < rows && listOffset + i < actors.size(); i++) {
            SyncMsg.ActorInfo a = actors.get(listOffset + i);
            Component label = Component.literal(a.name()).append(Component.literal("   " + TimelineSlider.time(a.length())).withStyle(s -> s.withColor(0xA0A0A0)));
            btn(label, rx, listTop + i * p, bw, () -> minecraft.setScreen(new ActorScreen(a.id())));
        }
        if (scroll) {
            btn(Component.literal("^"), rx + rw - 20, listTop, 20, () -> {
                listOffset = Math.max(0, listOffset - 1);
                refresh();
            });
            btn(Component.literal("v"), rx + rw - 20, listTop + (rows - 1) * p, 20, () -> {
                listOffset++;
                refresh();
            });
        }
        int ny = listTop + rows * p;
        actorBox = addRenderableWidget(new EditBox(font, rx + 1, ny + 1, rw - 96, 18, tr("cinematic.studio.new_actor")));
        actorBox.setMaxLength(32);
        actorBox.setHint(tr("cinematic.studio.new_actor"));
        btn(tr("cinematic.studio.add_actor"), rx + rw - 90, ny, 90, () -> {
            ClientStudio.send(Action.CREATE_ACTOR, null, actorBox.getValue().trim(), 0);
            actorBox.setValue("");
        }).active = can && scene != null;

        // Playback
        int ty = bottomTop + 15;
        timeline = addRenderableWidget(new TimelineSlider(m, ty, width - 2 * m, 20, () -> ClientStudio.sceneLength(ClientStudio.selectedScene)));
        int n = 4, w = (width - 2 * m - (n - 1) * Ui.GAP) / n, by = ty + 26, x = m;
        boolean running = ClientStudio.serverPlaying() || ClientStudio.isRecording() || ClientStudio.play().countdown() > 0;
        btn(tr(running ? "cinematic.studio.stop" : "cinematic.studio.play"), x, by, w, () -> {
            if (running) {
                ClientStudio.stopOnServer();
                refresh();
            } else {
                ClientStudio.playOnServer();
                onClose();
            }
        }).active = can && scene != null;
        x += w + Ui.GAP;
        btn(tr(ClientStudio.previewing ? "cinematic.studio.preview_pause" : "cinematic.studio.preview"), x, by, w, () -> {
            if (!ClientStudio.serverPlaying()) {
                if (!ClientStudio.previewing && ClientStudio.cursor >= ClientStudio.sceneLength(ClientStudio.selectedScene)) ClientStudio.cursor = 0;
                ClientStudio.previewing = !ClientStudio.previewing;
            }
            refresh();
        });
        x += w + Ui.GAP;
        btn(tr("cinematic.studio.director"), x, by, w, () -> minecraft.setScreen(new DirectorScreen()));
        x += w + Ui.GAP;
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
        g.fill(0, 0, width, height, 0x70000000);
        int m = Ui.MARGIN;
        Ui.header(g, font, width, title, tr("cinematic.studio.subtitle"));
        Ui.panel(g, m, Ui.TOP, lw, bottomTop - 12 - Ui.TOP);
        Ui.panel(g, rx, Ui.TOP, rw, bottomTop - 12 - Ui.TOP);
        Ui.panel(g, m, bottomTop, width - 2 * m, 61);
        Ui.section(g, font, m, Ui.TOP, lw, tr("cinematic.studio.section_scene"));
        Ui.section(g, font, rx, Ui.TOP, rw, tr("cinematic.studio.actors"));
        Ui.section(g, font, m, bottomTop, width - 2 * m, tr("cinematic.studio.section_play"));

        String sceneName = ClientStudio.selectedScene.isEmpty() ? "-" : ClientStudio.selectedScene;
        g.drawCenteredString(font, Component.literal(sceneName), m + lw / 2, Ui.TOP + 21, Ui.TITLE);

        Component help;
        int color = Ui.HINT;
        if (!ClientStudio.serverHasMod()) {
            help = tr("cinematic.studio.no_server_mod");
            color = 0xFFFF7070;
        } else if (!ClientStudio.canDirect()) {
            help = tr("cinematic.studio.no_permission");
            color = 0xFFFF9090;
        } else if (ClientStudio.scene(ClientStudio.selectedScene) == null) {
            help = tr("cinematic.studio.make_scene_help");
        } else if (ClientStudio.serverPlaying()) {
            help = tr("cinematic.studio.playing", ClientStudio.play().scene());
            color = 0xFF80FF80;
        } else {
            help = tr("cinematic.studio.pick_actor");
        }
        if (helpY + 10 < bottomTop - 12) g.drawWordWrap(font, help, m, helpY, lw, color);
        if (ClientStudio.scene(ClientStudio.selectedScene) != null && ClientStudio.scene(ClientStudio.selectedScene).actors().isEmpty()) {
            g.drawWordWrap(font, tr("cinematic.studio.no_actor_yet"), rx, Ui.TOP + 21, rw, Ui.HINT);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
