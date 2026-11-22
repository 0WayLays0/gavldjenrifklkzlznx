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
        return Math.min(150, Math.max(96, width / 4));
    }

    // Layout, computed in init and reused in render
    private int x1, cw, secLook, secTake, secMore, infoY;

    @Override
    protected void init() {
        SyncMsg.ActorInfo a = ClientStudio.actor(actor);
        if (a == null) {
            minecraft.setScreen(new StudioScreen());
            return;
        }
        boolean can = ClientStudio.canDirect() && ClientStudio.serverHasMod();
        int m = Ui.MARGIN, p = Ui.pitch(height);
        x1 = m + boxW() + 18;
        int total = width - x1 - m;
        cw = (total - Ui.GAP) / 2;
        int x2 = x1 + cw + Ui.GAP;
        btn(tr("cinematic.studio.back"), width - m - 110, 6, 110, () -> minecraft.setScreen(new StudioScreen()));
        btn(confirmDelete ? tr("cinematic.studio.confirm") : tr("cinematic.studio.delete_actor"), width - m - 110 - Ui.GAP - 110, 6, 110, () -> {
            if (!confirmDelete) {
                confirmDelete = true;
                refresh();
                return;
            }
            ClientStudio.send(Action.DELETE_ACTOR, actor, "", 0);
            minecraft.setScreen(new StudioScreen());
        }).active = can;

        // Look
        secLook = Ui.TOP;
        int y = secLook + 15;
        nameBox = addRenderableWidget(new EditBox(font, x1 + 1, y + 1, cw - 2, 18, tr("cinematic.studio.name")));
        nameBox.setMaxLength(32);
        nameBox.setValue(a.name());
        btn(tr("cinematic.studio.rename"), x2, y, cw, () -> ClientStudio.send(Action.RENAME_ACTOR, actor, nameBox.getValue(), 0)).active = can;
        y += p;
        btn(tr(a.slim() ? "cinematic.studio.arms_slim" : "cinematic.studio.arms_wide"), x1, y, cw,
                () -> ClientStudio.send(Action.SET_SLIM, actor, "", a.slim() ? 0 : 1)).active = can;
        btn(tr("cinematic.studio.next_skin"), x2, y, cw, this::nextFolderSkin).active = can;
        y += p;
        btn(tr("cinematic.studio.open_folder"), x1, y, cw, () -> Util.getPlatform().openFile(SkinCache.skinsFolder().toFile()));
        btn(tr("cinematic.studio.reset_skin"), x2, y, cw, () -> ClientStudio.send(Action.SET_SKIN, actor, "", 0)).active = can && !a.skin().isEmpty();

        // Take
        secTake = y + p + 6;
        y = secTake + 15;
        btn(tr("cinematic.studio.record"), x1, y, cw, () -> {
            ClientStudio.send(Action.RECORD, actor, "", 0);
            onClose();
        }).active = can && !ClientStudio.isRecording();
        btn(tr("cinematic.studio.poses"), x2, y, cw, () -> minecraft.setScreen(new PoseEditorScreen(actor))).active = a.length() > 0;
        y += p;
        btn(tr("cinematic.studio.clear_take"), x1, y, cw, () -> ClientStudio.send(Action.CLEAR_TAKE, actor, "", 0)).active = can && a.length() > 0;
        btn(tr("cinematic.studio.move_here"), x2, y, cw, () -> ClientStudio.send(Action.MOVE_HERE, actor, "", 0)).active = can;

        // More
        secMore = y + p + 6;
        y = secMore + 15;
        btn(tr("cinematic.studio.duplicate"), x1, y, cw, () -> ClientStudio.send(Action.DUPLICATE_ACTOR, actor, "", 0)).active = can;
        boolean filmed = actor.equals(ClientStudio.autoTarget);
        btn(tr(filmed ? "cinematic.studio.auto_target_on" : "cinematic.studio.auto_target_off"), x2, y, cw, () -> {
            ClientStudio.autoTarget = filmed ? null : actor;
            refresh();
        });
        infoY = y + p + 4;
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
        g.fill(0, 0, width, height, 0x70000000);
        SyncMsg.ActorInfo a = ClientStudio.actor(actor);
        if (a == null) return;
        int m = Ui.MARGIN;
        Ui.header(g, font, width, title.copy().append(" : " + a.name()), tr("cinematic.actor.subtitle"));
        int bx = m, by = Ui.TOP, bw = boxW(), bh = height - by - m;
        boolean hover = mx >= bx && mx < bx + bw && my >= by && my < by + bh;
        Ui.panel(g, bx, by, bw, bh);
        g.fill(bx, by, bx + bw, by + bh, hover ? 0x30FFFFFF : 0x18FFFFFF);
        Ui.section(g, font, bx, by, bw, tr("cinematic.actor.section_skin"));
        g.drawString(font, tr("cinematic.studio.info_take", TimelineSlider.time(a.length())), bx, by + 16, Ui.TEXT);
        g.drawString(font, tr("cinematic.studio.info_blocks", a.blocks(), a.keys()), bx, by + 28, Ui.HINT);
        var e = ClientStudio.findEntity(actor);
        int scale = Math.max(20, Math.min(80, (bh - 80) / 2));
        if (e != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, bx + bw / 2, by + bh - 34, scale, bx + bw / 2f - mx, by + bh / 3f - my, e);
        } else {
            g.drawWordWrap(font, tr("cinematic.studio.too_far"), bx + 4, by + bh / 2, bw - 8, Ui.HINT);
        }
        g.drawCenteredString(font, tr("cinematic.studio.drop_here"), bx + bw / 2, by + bh - 24, 0xFFFFFFFF);
        g.drawCenteredString(font, tr("cinematic.studio.drop_sizes"), bx + bw / 2, by + bh - 12, Ui.HINT);

        int total = width - x1 - m;
        Ui.panel(g, x1, Ui.TOP, total, height - Ui.TOP - m);
        Ui.section(g, font, x1, secLook, total, tr("cinematic.actor.section_look"));
        Ui.section(g, font, x1, secTake, total, tr("cinematic.actor.section_take"));
        Ui.section(g, font, x1, secMore, total, tr("cinematic.actor.section_more"));
        g.drawWordWrap(font, status, x1, infoY, total, statusColor);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
