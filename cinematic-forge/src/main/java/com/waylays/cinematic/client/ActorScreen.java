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

/** One actor: its skin (drop a PNG anywhere on this screen), arms, takes, poses. */
public class ActorScreen extends Screen {
    private static int folderIndex = -1;
    private final UUID actor;
    private EditBox nameBox;
    private Component status = Component.empty();
    private int statusColor = 0xFFFFFF;
    private boolean confirmDelete;

    public ActorScreen(UUID actor) {
        super(Component.translatable("cinematic.actor.title"));
        this.actor = actor;
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    private Button btn(Component label, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(label, b -> action.run()).bounds(x, y, w, 20).build());
    }

    void refresh() {
        String name = nameBox == null ? null : nameBox.getValue();
        rebuildWidgets();
        if (name != null) nameBox.setValue(name);
    }

    private int boxW() {
        return Math.min(140, Math.max(90, width / 4));
    }

    @Override
    protected void init() {
        SyncMsg.ActorInfo a = ClientStudio.actor(actor);
        if (a == null) {
            minecraft.setScreen(new StudioScreen());
            return;
        }
        boolean can = ClientStudio.canDirect() && ClientStudio.serverHasMod();
        int x1 = 8 + boxW() + 8;
        int cw = Math.min(170, (width - x1 - 8 - 4) / 2), x2 = x1 + cw + 4;
        int y = 22;
        nameBox = addRenderableWidget(new EditBox(font, x1 + 1, y + 1, cw - 2, 18, tr("cinematic.studio.name")));
        nameBox.setMaxLength(32);
        nameBox.setValue(a.name());
        btn(tr("cinematic.studio.rename"), x2, y, cw, () -> ClientStudio.send(Action.RENAME_ACTOR, actor, nameBox.getValue(), 0)).active = can;
        y += 24;
        btn(tr(a.slim() ? "cinematic.studio.arms_slim" : "cinematic.studio.arms_wide"), x1, y, cw,
                () -> ClientStudio.send(Action.SET_SLIM, actor, "", a.slim() ? 0 : 1)).active = can;
        btn(tr("cinematic.studio.next_skin"), x2, y, cw, this::nextFolderSkin).active = can;
        y += 22;
        btn(tr("cinematic.studio.open_folder"), x1, y, cw, () -> Util.getPlatform().openFile(SkinCache.skinsFolder().toFile()));
        btn(tr("cinematic.studio.reset_skin"), x2, y, cw, () -> ClientStudio.send(Action.SET_SKIN, actor, "", 0)).active = can && !a.skin().isEmpty();
        y += 22;
        btn(tr("cinematic.studio.record"), x1, y, cw, () -> {
            ClientStudio.send(Action.RECORD, actor, "", 0);
            onClose();
        }).active = can && !ClientStudio.isRecording();
        btn(tr("cinematic.studio.poses"), x2, y, cw, () -> minecraft.setScreen(new PoseEditorScreen(actor))).active = a.length() > 0;
        y += 22;
        btn(tr("cinematic.studio.clear_take"), x1, y, cw, () -> ClientStudio.send(Action.CLEAR_TAKE, actor, "", 0)).active = can && a.length() > 0;
        btn(tr("cinematic.studio.move_here"), x2, y, cw, () -> ClientStudio.send(Action.MOVE_HERE, actor, "", 0)).active = can;
        y += 22;
        btn(tr("cinematic.studio.duplicate"), x1, y, cw, () -> ClientStudio.send(Action.DUPLICATE_ACTOR, actor, "", 0)).active = can;
        boolean filmed = actor.equals(ClientStudio.autoTarget);
        btn(tr(filmed ? "cinematic.studio.auto_target_on" : "cinematic.studio.auto_target_off"), x2, y, cw, () -> {
            ClientStudio.autoTarget = filmed ? null : actor;
            refresh();
        });
        y += 22;
        btn(confirmDelete ? tr("cinematic.studio.confirm") : tr("cinematic.studio.delete_actor"), x1, y, cw, () -> {
            if (!confirmDelete) {
                confirmDelete = true;
                refresh();
                return;
            }
            ClientStudio.send(Action.DELETE_ACTOR, actor, "", 0);
            minecraft.setScreen(new StudioScreen());
        }).active = can;
        btn(tr("cinematic.studio.back"), x2, y, cw, () -> minecraft.setScreen(new StudioScreen()));
    }

    private void nextFolderSkin() {
        List<Path> files = SkinCache.folderSkins();
        if (files.isEmpty()) {
            say(tr("cinematic.skin.folder_empty"), 0xFFC060);
            return;
        }
        folderIndex = (folderIndex + 1) % files.size();
        giveSkin(files.get(folderIndex));
    }

    private void giveSkin(Path file) {
        String problem = SkinCache.upload(actor, file);
        if (problem != null) say(tr(problem), 0xFF7070);
        else say(tr("cinematic.skin.sent", file.getFileName().toString()), 0x80FF80);
    }

    private void say(Component c, int color) {
        status = c;
        statusColor = color;
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        for (Path f : files) {
            if (f.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
                giveSkin(f);
                return;
            }
        }
        say(tr("cinematic.skin.not_png"), 0xFF7070);
    }

    @Override
    public void tick() {
        if (nameBox != null) nameBox.tick();
        if (ClientStudio.actor(actor) == null) minecraft.setScreen(new StudioScreen());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x90000000);
        SyncMsg.ActorInfo a = ClientStudio.actor(actor);
        if (a == null) return;
        g.drawString(font, title.copy().append(" : " + a.name()), 8, 8, 0xFFFFFF);
        int bx = 8, by = 22, bw = boxW(), bh = height - by - 8;
        boolean hover = mx >= bx && mx < bx + bw && my >= by && my < by + bh;
        g.fill(bx, by, bx + bw, by + bh, hover ? 0x50FFFFFF : 0x30FFFFFF);
        g.renderOutline(bx, by, bw, bh, 0xFFFFE070);
        var e = ClientStudio.findEntity(actor);
        int scale = Math.max(20, Math.min(70, (bh - 30) / 2));
        if (e != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, bx + bw / 2, by + bh - 16, scale, bx + bw / 2f - mx, by + bh / 3f - my, e);
        } else {
            g.drawCenteredString(font, tr("cinematic.studio.too_far"), bx + bw / 2, by + bh / 2, 0xA0A0A0);
        }
        g.drawCenteredString(font, tr("cinematic.studio.drop_here"), bx + bw / 2, by + bh - 11, 0xFFFFFF);
        int x1 = 8 + bw + 8, iy = 22 + 24 + 22 * 6 + 4;
        g.drawString(font, tr("cinematic.studio.info_take", TimelineSlider.time(a.length())), x1, iy, 0xD0D0D0);
        g.drawString(font, tr("cinematic.studio.info_blocks", a.blocks(), a.keys()), x1, iy + 10, 0xD0D0D0);
        g.drawString(font, tr(a.skin().isEmpty() ? "cinematic.studio.info_default_skin" : "cinematic.studio.info_skin"), x1, iy + 20, 0xD0D0D0);
        g.drawString(font, status, x1, iy + 32, statusColor);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
