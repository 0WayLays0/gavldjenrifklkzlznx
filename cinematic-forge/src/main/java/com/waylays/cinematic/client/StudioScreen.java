package com.waylays.cinematic.client;

import com.waylays.cinematic.net.ActionMsg.Action;
import com.waylays.cinematic.net.SyncMsg;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** The studio: scenes, actors (skin by drag and drop, recording, poses), playback and the cameras. */
public class StudioScreen extends Screen {
    private static UUID selected;
    private static int listOffset;
    private static int folderIndex = -1;

    private EditBox sceneBox, actorBox, renameBox;
    private TimelineSlider timeline;
    private Component status = Component.empty();
    private int statusColor = 0xFFFFFF;
    private String confirm = "";

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
        String r = renameBox == null ? null : renameBox.getValue();
        UUID before = selected;
        rebuildWidgets();
        sceneBox.setValue(s);
        actorBox.setValue(a);
        if (renameBox != null && r != null && before != null && before.equals(selected)) renameBox.setValue(r);
    }

    private void say(Component c, int color) {
        status = c;
        statusColor = color;
    }

    @Override
    protected void init() {
        SyncMsg.SceneInfo scene = ClientStudio.scene(ClientStudio.selectedScene);
        if (scene == null || scene.actors().stream().noneMatch(a -> a.id().equals(selected))) selected = null;
        boolean can = ClientStudio.canDirect() && ClientStudio.serverHasMod();
        int left = 8, top = 24;

        // Scenes
        btn(Component.literal("<"), left, top, 20, () -> cycleScene(-1));
        btn(Component.literal(">"), left + 140, top, 20, () -> cycleScene(1));
        sceneBox = addRenderableWidget(new EditBox(font, left, top + 24, 104, 18, tr("cinematic.studio.new_scene")));
        sceneBox.setMaxLength(32);
        sceneBox.setHint(tr("cinematic.studio.new_scene"));
        btn(tr("cinematic.studio.add"), left + 108, top + 23, 52, () -> {
            if (sceneBox.getValue().isBlank()) return;
            ClientStudio.selectedScene = sceneBox.getValue().trim();
            ClientStudio.send(Action.CREATE_SCENE, null, sceneBox.getValue().trim(), 0);
            sceneBox.setValue("");
        }).active = can;
        btn(confirm.equals("scene") ? tr("cinematic.studio.confirm") : tr("cinematic.studio.delete_scene"), left, top + 46, 160, () -> {
            if (!confirm.equals("scene")) {
                confirm = "scene";
                refresh();
                return;
            }
            confirm = "";
            ClientStudio.send(Action.DELETE_SCENE, null, "", 0);
        }).active = can && scene != null;

        // Actors
        int listTop = top + 72;
        int rows = Math.max(1, (height - listTop - 104) / 22);
        List<SyncMsg.ActorInfo> actors = scene == null ? List.of() : scene.actors();
        listOffset = Math.max(0, Math.min(listOffset, actors.size() - rows));
        for (int i = 0; i < rows && listOffset + i < actors.size(); i++) {
            SyncMsg.ActorInfo a = actors.get(listOffset + i);
            String label = (a.id().equals(selected) ? "> " : "") + a.name() + "  " + TimelineSlider.time(a.length());
            btn(Component.literal(label), left, listTop + i * 22, 140, () -> {
                selected = a.id();
                confirm = "";
                refresh();
            });
        }
        if (actors.size() > rows) {
            btn(Component.literal("^"), left + 142, listTop, 18, () -> {
                listOffset = Math.max(0, listOffset - 1);
                refresh();
            });
            btn(Component.literal("v"), left + 142, listTop + (rows - 1) * 22, 18, () -> {
                listOffset++;
                refresh();
            });
        }
        int below = listTop + rows * 22 + 2;
        actorBox = addRenderableWidget(new EditBox(font, left, below + 1, 104, 18, tr("cinematic.studio.new_actor")));
        actorBox.setMaxLength(32);
        actorBox.setHint(tr("cinematic.studio.new_actor"));
        btn(tr("cinematic.studio.add_actor"), left + 108, below, 52, () -> {
            ClientStudio.send(Action.CREATE_ACTOR, null, actorBox.getValue().trim(), 0);
            actorBox.setValue("");
        }).active = can && scene != null;

        // The selected actor
        renameBox = null;
        SyncMsg.ActorInfo a = ClientStudio.actor(selected);
        if (a != null) {
            int rx = 176;
            renameBox = addRenderableWidget(new EditBox(font, rx, top + 1, 120, 18, tr("cinematic.studio.name")));
            renameBox.setMaxLength(32);
            renameBox.setValue(a.name());
            btn(tr("cinematic.studio.rename"), rx + 124, top, 70, () -> ClientStudio.send(Action.RENAME_ACTOR, a.id(), renameBox.getValue(), 0)).active = can;
            int cx = rx + 108, cw = Math.max(100, Math.min(160, (width - cx - 12) / 2));
            int cx2 = cx + cw + 4;
            int y = top + 24;
            btn(tr(a.slim() ? "cinematic.studio.arms_slim" : "cinematic.studio.arms_wide"), cx, y, cw,
                    () -> ClientStudio.send(Action.SET_SLIM, a.id(), "", a.slim() ? 0 : 1)).active = can;
            btn(tr("cinematic.studio.next_skin"), cx, y + 22, cw, () -> nextFolderSkin(a.id())).active = can;
            btn(tr("cinematic.studio.open_folder"), cx, y + 44, cw, () -> Util.getPlatform().openFile(SkinCache.skinsFolder().toFile()));
            btn(tr("cinematic.studio.record"), cx, y + 66, cw, () -> {
                ClientStudio.send(Action.RECORD, a.id(), "", 0);
                onClose();
            }).active = can && !ClientStudio.isRecording();
            btn(tr("cinematic.studio.clear_take"), cx, y + 88, cw, () -> ClientStudio.send(Action.CLEAR_TAKE, a.id(), "", 0)).active = can && a.length() > 0;
            btn(tr("cinematic.studio.move_here"), cx, y + 110, cw, () -> ClientStudio.send(Action.MOVE_HERE, a.id(), "", 0)).active = can;
            btn(tr("cinematic.studio.poses"), cx2, y, cw, () -> minecraft.setScreen(new PoseEditorScreen(a.id()))).active = a.length() > 0;
            btn(tr("cinematic.studio.duplicate"), cx2, y + 22, cw, () -> ClientStudio.send(Action.DUPLICATE_ACTOR, a.id(), "", 0)).active = can;
            boolean filmed = a.id().equals(ClientStudio.autoTarget);
            btn(tr(filmed ? "cinematic.studio.auto_target_on" : "cinematic.studio.auto_target_off"), cx2, y + 44, cw, () -> {
                ClientStudio.autoTarget = filmed ? null : a.id();
                refresh();
            });
            btn(tr("cinematic.studio.reset_skin"), cx2, y + 66, cw, () -> ClientStudio.send(Action.SET_SKIN, a.id(), "", 0)).active = can && !a.skin().isEmpty();
            btn(confirm.equals("actor") ? tr("cinematic.studio.confirm") : tr("cinematic.studio.delete_actor"), cx2, y + 88, cw, () -> {
                if (!confirm.equals("actor")) {
                    confirm = "actor";
                    refresh();
                    return;
                }
                confirm = "";
                ClientStudio.send(Action.DELETE_ACTOR, a.id(), "", 0);
                selected = null;
            }).active = can;
        }

        // Playback and cameras
        timeline = addRenderableWidget(new TimelineSlider(8, height - 50, width - 16, 20, () -> ClientStudio.sceneLength(ClientStudio.selectedScene)));
        int n = 7, bw = Math.min(110, (width - 16 - (n - 1) * 3) / n), by = height - 26;
        int x = 8;
        btn(tr("cinematic.studio.play"), x, by, bw, () -> {
            ClientStudio.playOnServer();
            onClose();
        }).active = can && scene != null && !ClientStudio.isRecording();
        x += bw + 3;
        btn(tr("cinematic.studio.stop"), x, by, bw, ClientStudio::stopOnServer).active = can;
        x += bw + 3;
        btn(tr(ClientStudio.previewing ? "cinematic.studio.preview_pause" : "cinematic.studio.preview"), x, by, bw, () -> {
            if (!ClientStudio.serverPlaying()) {
                if (!ClientStudio.previewing && ClientStudio.cursor >= ClientStudio.sceneLength(ClientStudio.selectedScene)) ClientStudio.cursor = 0;
                ClientStudio.previewing = !ClientStudio.previewing;
            }
            refresh();
        });
        x += bw + 3;
        btn(tr("cinematic.studio.director"), x, by, bw, () -> minecraft.setScreen(new DirectorScreen()));
        x += bw + 3;
        btn(tr(StudioCamera.free() ? "cinematic.studio.freecam_on" : "cinematic.studio.freecam_off"), x, by, bw, () -> {
            StudioCamera.toggleFree(minecraft);
            refresh();
        });
        x += bw + 3;
        btn(tr(StudioCamera.filmCamera ? "cinematic.studio.filmcam_on" : "cinematic.studio.filmcam_off"), x, by, bw, () -> {
            StudioCamera.filmCamera = !StudioCamera.filmCamera;
            refresh();
        });
        x += bw + 3;
        btn(tr("cinematic.studio.emotes"), x, by, bw, () -> minecraft.setScreen(new EmoteScreen()));
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
        selected = null;
        listOffset = 0;
        confirm = "";
        refresh();
    }

    private void nextFolderSkin(UUID actor) {
        List<Path> files = SkinCache.folderSkins();
        if (files.isEmpty()) {
            say(tr("cinematic.skin.folder_empty"), 0xFFC060);
            return;
        }
        folderIndex = (folderIndex + 1) % files.size();
        giveSkin(actor, files.get(folderIndex));
    }

    private void giveSkin(UUID actor, Path file) {
        String problem = SkinCache.upload(actor, file);
        if (problem != null) say(tr(problem), 0xFF7070);
        else say(tr("cinematic.skin.sent", file.getFileName().toString()), 0x80FF80);
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        if (selected == null) {
            say(tr("cinematic.skin.select_first"), 0xFFC060);
            return;
        }
        for (Path f : files) {
            if (f.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
                giveSkin(selected, f);
                return;
            }
        }
        say(tr("cinematic.skin.not_png"), 0xFF7070);
    }

    @Override
    public void tick() {
        sceneBox.tick();
        actorBox.tick();
        if (renameBox != null) renameBox.tick();
        if (!timeline.isFocused() || ClientStudio.serverPlaying() || ClientStudio.previewing) timeline.sync();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x90000000);
        g.drawString(font, title, 8, 8, 0xFFFFFF);
        String sceneName = ClientStudio.selectedScene.isEmpty() ? "-" : ClientStudio.selectedScene;
        g.drawCenteredString(font, Component.literal(sceneName), 8 + 80, 30, 0xFFE070);

        if (!ClientStudio.serverHasMod()) {
            g.drawCenteredString(font, tr("cinematic.studio.no_server_mod"), width / 2, height / 2, 0xFF7070);
        } else if (!ClientStudio.canDirect()) {
            g.drawString(font, tr("cinematic.studio.no_permission"), 176, 8, 0xFF9090);
        }

        SyncMsg.ActorInfo a = ClientStudio.actor(selected);
        if (a != null) {
            int bx = 176, by = 48, bw = 100, bh = 132;
            boolean hover = mx >= bx && mx < bx + bw && my >= by && my < by + bh;
            g.fill(bx, by, bx + bw, by + bh, hover ? 0x60FFFFFF : 0x40FFFFFF);
            g.renderOutline(bx, by, bw, bh, 0xFFFFE070);
            var e = ClientStudio.findEntity(a.id());
            if (e != null) {
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, bx + bw / 2, by + bh - 14, 50, bx + bw / 2f - mx, by + 40 - my, e);
            } else {
                g.drawCenteredString(font, tr("cinematic.studio.too_far"), bx + bw / 2, by + 50, 0xA0A0A0);
            }
            g.drawCenteredString(font, tr("cinematic.studio.drop_here"), bx + bw / 2, by + bh - 10, 0xFFFFFF);
            int iy = by + bh + 4;
            g.drawString(font, tr("cinematic.studio.info_take", TimelineSlider.time(a.length())), bx, iy, 0xD0D0D0);
            g.drawString(font, tr("cinematic.studio.info_blocks", a.blocks(), a.keys()), bx, iy + 10, 0xD0D0D0);
            g.drawString(font, tr(a.skin().isEmpty() ? "cinematic.studio.info_default_skin" : "cinematic.studio.info_skin"), bx, iy + 20, 0xD0D0D0);
        } else if (ClientStudio.scene(ClientStudio.selectedScene) != null) {
            g.drawString(font, tr("cinematic.studio.pick_actor"), 176, 30, 0xA0A0A0);
        } else {
            g.drawString(font, tr("cinematic.studio.make_scene"), 176, 30, 0xA0A0A0);
        }
        if (ClientStudio.serverPlaying()) {
            g.drawString(font, tr("cinematic.studio.playing", ClientStudio.play().scene()), 8, height - 64, 0x80FF80);
        }
        g.drawString(font, status, 8, height - 76, statusColor);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
