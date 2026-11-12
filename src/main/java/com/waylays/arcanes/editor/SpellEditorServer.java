package com.waylays.arcanes.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The spell editor on the server: {@code /arcanes sorts} opens it for a game master (permission level 2); the changes
 * it sends are checked and kept, Iron's parameters in Iron's own spell config files
 * ({@code config/irons_spellbooks_spell_config/<mod>/<spell>.json}, as if written by hand), the editor's own stats in
 * its file; then Iron's config is read again and everything is sent to every player.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class SpellEditorServer {
    /** The permission level needed to edit spells. */
    public static final int PERMISSION = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private SpellEditorServer() {}

    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        SpellTuning.load();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        // The spells' own values back (another world, or a server, may tune them differently)
        SpellTuning.replace(Map.of());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, SpellEditorNet.Sync.now());
        }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("arcanes")
                .then(Commands.literal("sorts")
                        .requires(source -> source.hasPermission(PERMISSION))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PacketDistributor.sendToPlayer(player, SpellEditorNet.Sync.now());
                            PacketDistributor.sendToPlayer(player, new SpellEditorNet.Open());
                            return 1;
                        })));
    }

    /** A game master's changes (see {@link SpellEditorNet.Edit}). */
    static void edit(ServerPlayer player, SpellEditorNet.Edit edit) {
        if (!player.hasPermissions(PERMISSION)) {
            player.sendSystemMessage(Component.translatable("ui.arcanes_oublies.editor.denied"));
            return;
        }
        MinecraftServer server = player.server;
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        List<String> errors = new ArrayList<>();
        Map<ResourceLocation, Map<SpellTuning.Stat, Double>> tuning = new HashMap<>();
        SpellTuning.all().forEach((id, m) -> tuning.put(id, new EnumMap<>(m)));
        int changed = 0;
        for (ResourceLocation id : edit.spells()) {
            AbstractSpell spell = SpellRegistry.getSpell(id);
            if (spell == null || spell == SpellRegistry.none()) {
                errors.add(id + ": ?");
                continue;
            }
            changed++;
            writeIronConfig(spell, edit, ops, errors);
            Map<SpellTuning.Stat, Double> m = tuning.computeIfAbsent(id, k -> new EnumMap<>(SpellTuning.Stat.class));
            if (edit.reset()) {
                m.clear();
            }
            edit.tune().forEach((key, value) -> {
                SpellTuning.Stat stat = SpellTuning.Stat.byKey(key);
                if (stat == null) {
                    errors.add(key + ": ?");
                } else if (value == null || value.isNaN() || value.isInfinite()) {
                    m.remove(stat);
                } else {
                    double v = stat.clamp(value);
                    if (stat.inSpell() && v == SpellTuning.original(spell, stat) || !stat.inSpell() && v == stat.neutral()) {
                        m.remove(stat);
                    } else {
                        m.put(stat, v);
                    }
                }
            });
            if (m.isEmpty()) {
                tuning.remove(id);
            }
        }
        SpellTuning.replace(tuning);
        SpellTuning.save();
        // Iron's config read again from its files, and sent to everyone; then the editor's stats
        SpellConfigManager.getInstance().handleServerConfigUpdate();
        SpellConfigManager.onDatapackSync(new OnDatapackSyncEvent(server.getPlayerList(), null));
        PacketDistributor.sendToAllPlayers(SpellEditorNet.Sync.now());
        PacketDistributor.sendToPlayer(player, new SpellEditorNet.Saved(changed, errors));
        ArcanesOublies.LOGGER.info("Spell editor: {} changed {} spell(s)", player.getGameProfile().getName(), changed);
    }

    /** The spell's Iron config file, with {@code edit}'s parameters set or taken off; deleted when nothing is left in it. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void writeIronConfig(AbstractSpell spell, SpellEditorNet.Edit edit, RegistryOps<JsonElement> ops, List<String> errors) {
        if (edit.iron().isEmpty() && !edit.reset()) {
            return;
        }
        ResourceLocation id = spell.getSpellResource();
        Path dir = SpellConfigManager.getSpellConfigDir().toPath().resolve(id.getNamespace());
        Path file = dir.resolve(id.getPath() + ".json");
        JsonObject json = edit.reset() ? new JsonObject() : read(file);
        edit.iron().forEach((key, value) -> {
            SpellConfigParameter param = parameter(key);
            if (param == null) {
                errors.add(key + ": ?");
                return;
            }
            String full = param.key().toString();
            json.remove(full);
            json.remove(param.key().getPath());
            if (value.isEmpty()) {
                return;
            }
            try {
                JsonElement element = JsonParser.parseString(value);
                Object decoded = param.datatype().decode(ops, element).getOrThrow().getFirst();
                Object def = SpellConfigManager.getSpellDefaultConfigValue(spell, param);
                if (!decoded.equals(def)) {
                    json.add(full, element);
                }
            } catch (RuntimeException e) {
                errors.add(id + " " + key + ": " + e.getMessage());
            }
        });
        try {
            boolean empty = json.keySet().stream().allMatch(k -> k.startsWith("_"));
            if (empty) {
                Files.deleteIfExists(file);
            } else {
                Files.createDirectories(dir);
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    GSON.toJson(json, writer);
                }
            }
        } catch (IOException e) {
            errors.add(id + ": " + e.getMessage());
        }
    }

    private static JsonObject read(Path file) {
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null) {
                    return json;
                }
            } catch (IOException | RuntimeException e) {
                ArcanesOublies.LOGGER.warn("Spell editor: {} could not be read, it is written anew: {}", file, e.toString());
            }
        }
        return new JsonObject();
    }

    @Nullable
    static SpellConfigParameter<?> parameter(String key) {
        for (SpellConfigParameter<?> p : SpellConfigManager.ALL_TYPES) {
            if (p.key().toString().equals(key)) {
                return p;
            }
        }
        return null;
    }
}
