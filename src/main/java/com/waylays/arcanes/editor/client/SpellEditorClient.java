package com.waylays.arcanes.editor.client;

import net.minecraft.client.Minecraft;

import java.util.List;

/** The client's side of the spell editor's packets. */
public final class SpellEditorClient {
    private SpellEditorClient() {}

    public static void open() {
        Minecraft.getInstance().setScreen(new SpellEditorScreen());
    }

    /** The editor's stats came (on joining, after anyone's edit): an open editor shows them. */
    public static void synced() {
        if (Minecraft.getInstance().screen instanceof SpellEditorScreen screen) {
            screen.refresh();
        }
    }

    public static void saved(int spells, List<String> errors) {
        if (Minecraft.getInstance().screen instanceof SpellEditorScreen screen) {
            screen.onSaved(spells, errors);
        }
    }
}
