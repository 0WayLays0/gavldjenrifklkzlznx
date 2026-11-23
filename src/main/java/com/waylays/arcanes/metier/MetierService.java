package com.waylays.arcanes.metier;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.metier.MetierDefs.Def;
import com.waylays.arcanes.metier.MetierDefs.Game;
import com.waylays.arcanes.metier.MetierDefs.Ingredient;
import com.waylays.arcanes.metier.MetierDefs.Outcome;
import com.waylays.arcanes.metier.MetierDefs.Recipe;
import com.waylays.arcanes.metier.MetierDefs.Reward;
import com.waylays.arcanes.metier.MetierDefs.Tool;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The trades on the server. A right click on a trade's block, with one of its tools, starts its minigame on the
 * player's screen (a station first lists its recipes); the result the client sends back is checked (the player still
 * there, the block and the tool still the same, the minigame not over too fast) and settled: the tool's wear, what the
 * outcome gives (or, at a station, the ingredients used and what is made), and the deposit worked out for a while
 * (the block turned into another, then given back). A right click on a worked-out deposit tells when it comes back.
 * In survival the trades' blocks cannot simply be broken.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class MetierService {
    public static final int PERMISSION = 2;
    /** A minigame shorter than this is not believed. */
    private static final long MIN_MILLIS = 2500;
    private static final long MAX_MILLIS = 5 * 60 * 1000;
    static final double MAX_DISTANCE = 7;

    /** A minigame going on. */
    private record Session(int id, UUID player, String def, int recipe, ServerLevel level, BlockPos pos, BlockState state, Item tool, long started,
                           double difficulty) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /** Per player, per trade (or "trade#recipe"), the game time from which they may go again. */
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, StringBuilder> UPLOADS = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static int nextSession = 1;

    private MetierService() {}

    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        MetierDefs.load();
        SESSIONS.clear();
        COOLDOWNS.clear();
    }

    // ---------------------------------------------------------------- starting

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Deposits deposits = Deposits.get(level.getServer());
        Deposits.Spent spent = deposits.spent(level.dimension(), pos);
        if (spent != null) {
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                long left = Math.max(0, spent.backAt - level.getServer().overworld().getGameTime());
                Def def = MetierDefs.byId(spent.def);
                player.displayClientMessage(Component.translatable("metier.arcanes_oublies.spent", def == null ? spent.def : def.name, time(left / 20))
                        .withStyle(ChatFormatting.GOLD), true);
            }
            cancel(event);
            return;
        }
        Def def = MetierDefs.forBlock(state);
        if (def == null) {
            // The grindstone, with a worn weapon or tool in hand: its sharpening minigame (see Grindstone)
            if (Grindstone.wants(player, state) && !(player.isCreative() && player.isShiftKeyDown())) {
                cancel(event);
                if (event.getHand() == InteractionHand.MAIN_HAND && !SESSIONS.containsKey(player.getUUID())) {
                    Grindstone.start(player, level, pos, state);
                }
            }
            return;
        }
        // A game master building in creative, crouching, uses the block as usual
        if (player.isCreative() && player.isShiftKeyDown()) {
            return;
        }
        cancel(event);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (SESSIONS.containsKey(player.getUUID())) {
            return;
        }
        if (def.isStation()) {
            openStation(player, level, pos, def);
            return;
        }
        ItemStack held = player.getMainHandItem();
        Tool tool = def.tool(held);
        if (!def.tools.isEmpty() && tool == null) {
            if (def.game == MetierDefs.Game.MINING) {
                // In character: the ore will not come without a pick
                player.displayClientMessage(Component.translatable("metier.arcanes_oublies.need_pickaxe").withStyle(ChatFormatting.GRAY,
                        ChatFormatting.ITALIC), true);
            } else {
                player.displayClientMessage(Component.translatable("metier.arcanes_oublies.need_tool", toolNames(def)).withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        long wait = cooldownLeft(player, def.id, level);
        if (wait > 0) {
            player.displayClientMessage(Component.translatable("metier.arcanes_oublies.cooldown", time(wait / 20)).withStyle(ChatFormatting.GOLD), true);
            return;
        }
        double difficulty = def.difficulty * (tool == null ? 1 : tool.difficulty);
        begin(player, level, pos, state, def, -1, tool == null ? null : held.getItem(), difficulty, new ItemStack(state.getBlock().asItem()),
                tool == null ? ItemStack.EMPTY : held.copyWithCount(1), List.of(), def.name, tool == null ? "" : held.getHoverName().getString());
    }

    private static void cancel(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void begin(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, Def def, int recipe, @Nullable Item tool,
                              double difficulty, ItemStack subject, ItemStack toolStack, List<ItemStack> inputs, String title, String subtitle) {
        begin(player, level, pos, state, def.id, def.game, recipe, tool, difficulty, subject, toolStack, inputs, title, subtitle, (float) def.perfectAt,
                (float) def.criticalBelow);
    }

    static void begin(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, String defId, MetierDefs.Game game, int recipe,
                      @Nullable Item tool, double difficulty, ItemStack subject, ItemStack toolStack, List<ItemStack> inputs, String title, String subtitle,
                      float perfectAt, float criticalBelow) {
        int id = nextSession++;
        SESSIONS.put(player.getUUID(), new Session(id, player.getUUID(), defId, recipe, level, pos.immutable(), state, tool, System.currentTimeMillis(),
                difficulty));
        PacketDistributor.sendToPlayer(player, new MetierNet.Start(id, game.ordinal(), difficulty, RANDOM.nextLong(), title, subtitle, subject,
                toolStack, inputs, perfectAt, criticalBelow));
    }

    private static Component toolNames(Def def) {
        List<String> names = new ArrayList<>();
        for (Tool t : def.tools) {
            names.add(new ItemStack(MetierDefs.item(t.item)).getHoverName().getString());
        }
        return Component.literal(String.join(", ", names));
    }

    /** "4 min 12 s". */
    public static Component time(long seconds) {
        if (seconds >= 3600) {
            return Component.translatable("metier.arcanes_oublies.time_h", seconds / 3600, (seconds % 3600) / 60);
        }
        if (seconds >= 60) {
            return Component.translatable("metier.arcanes_oublies.time_m", seconds / 60, seconds % 60);
        }
        return Component.translatable("metier.arcanes_oublies.time_s", Math.max(1, seconds));
    }

    private static long cooldownLeft(ServerPlayer player, String key, ServerLevel level) {
        Map<String, Long> m = COOLDOWNS.get(player.getUUID());
        if (m == null) {
            return 0;
        }
        return Math.max(0, m.getOrDefault(key, 0L) - level.getServer().overworld().getGameTime());
    }

    private static void setCooldown(ServerPlayer player, String key, int seconds) {
        if (seconds > 0) {
            COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                    .put(key, player.server.overworld().getGameTime() + seconds * 20L);
        }
    }

    // ---------------------------------------------------------------- stations

    private static void openStation(ServerPlayer player, ServerLevel level, BlockPos pos, Def def) {
        List<MetierNet.RecipeView> views = new ArrayList<>();
        for (int i = 0; i < def.recipes.size(); i++) {
            Recipe r = def.recipes.get(i);
            List<ItemStack> inputs = new ArrayList<>();
            List<Boolean> have = new ArrayList<>();
            for (Ingredient in : r.inputs) {
                inputs.add(new ItemStack(MetierDefs.item(in.item), in.count));
                have.add(count(player.getInventory(), MetierDefs.item(in.item)) >= in.count);
            }
            ItemStack partial = r.partial.isEmpty() ? ItemStack.EMPTY : MetierDefs.stack(r.partial.get(0), Math.max(1, r.partial.get(0).max),
                    level.registryAccess());
            ItemStack perfect = r.perfect.isEmpty() ? partial : MetierDefs.stack(r.perfect.get(0), Math.max(1, r.perfect.get(0).max),
                    level.registryAccess());
            int wait = (int) (cooldownLeft(player, def.id + "#" + i, level) / 20);
            views.add(new MetierNet.RecipeView(r.name, inputs, have, partial, perfect, def.difficulty * r.difficulty, wait,
                    def.folders.contains(r.folder) ? r.folder : ""));
        }
        BlockState state = level.getBlockState(pos);
        PacketDistributor.sendToPlayer(player, new MetierNet.Station(def.id, pos, def.name, def.game.ordinal(), new ItemStack(state.getBlock().asItem()),
                views, (int) (cooldownLeft(player, def.id, level) / 20), List.copyOf(def.folders)));
    }


    static void pick(ServerPlayer player, MetierNet.Pick pick) {
        Def def = MetierDefs.byId(pick.def());
        ServerLevel level = player.serverLevel();
        BlockPos pos = pick.pos();
        if (def == null || !def.enabled || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE * MAX_DISTANCE || !level.isLoaded(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!def.matches(state) || SESSIONS.containsKey(player.getUUID())) {
            return;
        }
        // A station is only used through its trade's recipes (its own screen is never opened)
        if (pick.recipe() < 0 || pick.recipe() >= def.recipes.size()) {
            return;
        }
        Recipe r = def.recipes.get(pick.recipe());
        for (Ingredient in : r.inputs) {
            if (count(player.getInventory(), MetierDefs.item(in.item)) < in.count) {
                player.displayClientMessage(Component.translatable("metier.arcanes_oublies.missing").withStyle(ChatFormatting.RED), true);
                return;
            }
        }
        long wait = Math.max(cooldownLeft(player, def.id, level), cooldownLeft(player, def.id + "#" + pick.recipe(), level));
        if (wait > 0) {
            player.displayClientMessage(Component.translatable("metier.arcanes_oublies.cooldown", time(wait / 20)).withStyle(ChatFormatting.GOLD), true);
            return;
        }
        ItemStack held = player.getMainHandItem();
        Tool tool = def.tool(held);
        if (!def.tools.isEmpty() && tool == null) {
            player.displayClientMessage(Component.translatable("metier.arcanes_oublies.need_tool", toolNames(def)).withStyle(ChatFormatting.RED), true);
            return;
        }
        List<ItemStack> inputs = new ArrayList<>();
        for (Ingredient in : r.inputs) {
            inputs.add(new ItemStack(MetierDefs.item(in.item), in.count));
        }
        ItemStack subject = r.perfect.isEmpty() ? (r.partial.isEmpty() ? new ItemStack(state.getBlock().asItem())
                : MetierDefs.stack(r.partial.get(0), 1, level.registryAccess())) : MetierDefs.stack(r.perfect.get(0), 1, level.registryAccess());
        double difficulty = def.difficulty * r.difficulty * (tool == null ? 1 : tool.difficulty);
        // The recipe's own minigame if it has one (a potion distilled at the healers' table), else the station's
        begin(player, level, pos, state, def.id, r.game != null ? r.game : def.game, pick.recipe(), tool == null ? null : held.getItem(), difficulty,
                subject, tool == null ? ItemStack.EMPTY : held.copyWithCount(1), inputs, r.name, def.name, (float) def.perfectAt, (float) def.criticalBelow);
    }

    private static int count(Inventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void take(Inventory inv, Item item, int count) {
        for (int i = 0; i < inv.getContainerSize() && count > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int n = Math.min(count, s.getCount());
                s.shrink(n);
                count -= n;
            }
        }
        inv.setChanged();
    }

    // ---------------------------------------------------------------- the result

    static void result(ServerPlayer player, MetierNet.Result result) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || s.id() != result.session()) {
            return;
        }
        SESSIONS.remove(player.getUUID());
        if (Grindstone.ID.equals(s.def())) {
            Grindstone.settle(player, s.id(), s.level(), s.pos(), s.state(), s.tool(), result.score(), result.critical(),
                    System.currentTimeMillis() - s.started() < MIN_MILLIS);
            return;
        }
        Def def = MetierDefs.byId(s.def());
        ServerLevel level = s.level();
        if (def == null || player.level() != level || player.distanceToSqr(s.pos().getCenter()) > MAX_DISTANCE * MAX_DISTANCE
                || !level.getBlockState(s.pos()).is(s.state().getBlock())) {
            PacketDistributor.sendToPlayer(player, new MetierNet.Stop(s.id(), "metier.arcanes_oublies.stop.gone"));
            return;
        }
        ItemStack held = player.getMainHandItem();
        boolean toolOk = s.tool() == null || held.is(s.tool());
        if (!toolOk) {
            PacketDistributor.sendToPlayer(player, new MetierNet.Stop(s.id(), "metier.arcanes_oublies.stop.tool"));
            return;
        }
        long took = System.currentTimeMillis() - s.started();
        float score = Math.max(0, Math.min(1, result.score()));
        Outcome outcome;
        if (result.critical() || took < MIN_MILLIS || score < def.criticalBelow) {
            outcome = Outcome.CRITICAL;
        } else if (score >= def.perfectAt) {
            outcome = Outcome.PERFECT;
        } else {
            outcome = Outcome.PARTIAL;
        }
        // The tool's wear
        int wear = 0;
        if (s.tool() != null && held.isDamageableItem()) {
            wear = outcome == Outcome.CRITICAL ? def.durabilityCritical : def.durability;
            if (wear > 0) {
                held.hurtAndBreak(wear, player, EquipmentSlot.MAINHAND);
            }
        }
        List<ItemStack> given;
        if (s.recipe() >= 0) {
            given = settleRecipe(player, def, s.recipe(), outcome);
            if (given == null) {
                PacketDistributor.sendToPlayer(player, new MetierNet.Stop(s.id(), "metier.arcanes_oublies.missing"));
                return;
            }
            setCooldown(player, def.id + "#" + s.recipe(), def.recipes.get(s.recipe()).cooldown);
        } else {
            List<Reward> rewards = switch (outcome) {
                case PERFECT -> def.perfect;
                case PARTIAL -> def.partial;
                case CRITICAL -> def.critical;
            };
            given = MetierDefs.roll(rewards, RANDOM, level.registryAccess());
            if (outcome != Outcome.CRITICAL || def.depleteOnCritical) {
                Deposits deposits = Deposits.get(level.getServer());
                if (deposits.work(level.dimension(), s.pos(), def.uses)) {
                    deplete(level, s.pos(), s.state(), def, deposits);
                }
            }
        }
        for (ItemStack stack : given) {
            player.getInventory().placeItemBackInInventory(stack.copy());
        }
        setCooldown(player, def.id, def.playerCooldown);
        level.playSound(null, s.pos(), switch (outcome) {
            case PERFECT -> SoundEvents.PLAYER_LEVELUP;
            case PARTIAL -> SoundEvents.ITEM_PICKUP;
            case CRITICAL -> SoundEvents.ITEM_BREAK;
        }, SoundSource.PLAYERS, .6f, outcome == Outcome.PERFECT ? 1.4f : 1f);
        PacketDistributor.sendToPlayer(player, new MetierNet.Outcome(s.id(), outcome.ordinal(), given, wear));
    }

    /** A station's recipe: ingredients used, what is made; null if the ingredients are no longer there. */
    @Nullable
    private static List<ItemStack> settleRecipe(ServerPlayer player, Def def, int index, Outcome outcome) {
        if (index >= def.recipes.size()) {
            return null;
        }
        Recipe r = def.recipes.get(index);
        for (Ingredient in : r.inputs) {
            if (count(player.getInventory(), MetierDefs.item(in.item)) < in.count) {
                return null;
            }
        }
        if (outcome != Outcome.CRITICAL || r.loseInputsOnFail) {
            for (Ingredient in : r.inputs) {
                take(player.getInventory(), MetierDefs.item(in.item), in.count);
            }
        }
        List<Reward> rewards = switch (outcome) {
            case PERFECT -> r.perfect;
            case PARTIAL -> r.partial;
            case CRITICAL -> r.critical;
        };
        return MetierDefs.roll(rewards, RANDOM, player.serverLevel().registryAccess());
    }

    private static void deplete(ServerLevel level, BlockPos pos, BlockState original, Def def, Deposits deposits) {
        BlockState into = MetierDefs.block(def.depletedBlock);
        level.setBlock(pos, into == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : into, 3);
        long back = level.getServer().overworld().getGameTime() + def.respawnSeconds * 20L;
        deposits.deplete(level.dimension(), pos, original, def.id, back);
    }

    // ---------------------------------------------------------------- the world

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        if (now % 20 != 0) {
            return;
        }
        // Deposits that come back
        Deposits deposits = Deposits.get(server);
        List<Deposits.Spent> due = new ArrayList<>();
        for (Deposits.Spent s : deposits.all()) {
            if (s.backAt <= now) {
                due.add(s);
            }
        }
        for (Deposits.Spent s : due) {
            ServerLevel level = server.getLevel(s.dim);
            if (level == null) {
                deposits.removeSpent(s);
                continue;
            }
            if (!level.isLoaded(s.pos)) {
                continue;
            }
            Def def = MetierDefs.byId(s.def);
            BlockState here = level.getBlockState(s.pos);
            BlockState spentState = def == null ? null : MetierDefs.block(def.depletedBlock);
            if (here.isAir() || spentState == null || here.is(spentState.getBlock())) {
                level.setBlock(s.pos, s.original, 3);
            }
            deposits.removeSpent(s);
        }
        // Minigames left hanging, or walked away from
        SESSIONS.values().removeIf(s -> {
            ServerPlayer p = server.getPlayerList().getPlayer(s.player());
            boolean gone = p == null || p.level() != s.level() || p.distanceToSqr(s.pos().getCenter()) > (MAX_DISTANCE + 3) * (MAX_DISTANCE + 3);
            boolean stale = System.currentTimeMillis() - s.started() > MAX_MILLIS;
            if ((gone || stale) && p != null) {
                PacketDistributor.sendToPlayer(p, new MetierNet.Stop(s.id(), "metier.arcanes_oublies.stop.gone"));
            }
            return gone || stale;
        });
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        Deposits deposits = Deposits.get(level.getServer());
        Deposits.Spent spent = deposits.spent(level.dimension(), event.getPos());
        if (player.isCreative()) {
            if (spent != null) {
                deposits.forget(level.dimension(), event.getPos());
            }
            return;
        }
        Def def = spent != null ? MetierDefs.byId(spent.def) : MetierDefs.forBlock(event.getState());
        if (def != null && !def.isStation() && def.protectBlocks) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable(spent != null ? "metier.arcanes_oublies.protected_spent" : "metier.arcanes_oublies.protected",
                    def.name).withStyle(ChatFormatting.GOLD), true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
        UPLOADS.remove(event.getEntity().getUUID());
    }

    // ---------------------------------------------------------------- the editor

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("arcanes").then(Commands.literal("metiers")
                .requires(s -> s.hasPermission(PERMISSION))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    PacketDistributor.sendToPlayer(p, new MetierNet.Editor(MetierDefs.toJson(MetierDefs.get())));
                    return 1;
                })
                .then(Commands.literal("recharger").executes(ctx -> {
                    MetierDefs.load();
                    ctx.getSource().sendSuccess(() -> Component.translatable("metier.arcanes_oublies.reloaded", MetierDefs.all().size()), true);
                    return 1;
                }))
                .then(Commands.literal("regenerer").executes(ctx -> regenerate(ctx.getSource().getServer(), null, 0, ctx.getSource()))
                        .then(Commands.argument("rayon", IntegerArgumentType.integer(1, 512)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            return regenerate(ctx.getSource().getServer(), p, IntegerArgumentType.getInteger(ctx, "rayon"), ctx.getSource());
                        })))));
    }

    /** Every worked-out deposit given back (or those within {@code radius} of {@code around}). */
    private static int regenerate(MinecraftServer server, @Nullable ServerPlayer around, int radius, net.minecraft.commands.CommandSourceStack source) {
        Deposits deposits = Deposits.get(server);
        int n = 0;
        for (Deposits.Spent s : new ArrayList<>(deposits.all())) {
            if (around != null && (around.level().dimension() != s.dim || around.blockPosition().distSqr(s.pos) > (double) radius * radius)) {
                continue;
            }
            ServerLevel level = server.getLevel(s.dim);
            if (level != null && level.isLoaded(s.pos)) {
                level.setBlock(s.pos, s.original, 3);
                deposits.removeSpent(s);
                n++;
            }
        }
        int done = n;
        source.sendSuccess(() -> Component.translatable("metier.arcanes_oublies.regenerated", done), true);
        return n;
    }

    /** The editor's user's held item, as it is made, sent back for the grindstone's list (the editor saves the list). */
    static void grindAsk(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!player.hasPermissions(PERMISSION) || held.isEmpty()) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new MetierNet.GrindItemView(MetierDefs.toJson(Grindstone.signature(player.server.registryAccess(), held))));
    }

    static void save(ServerPlayer player, MetierNet.Save save) {
        if (!player.hasPermissions(PERMISSION)) {
            return;
        }
        StringBuilder sb = save.part() == 0 ? new StringBuilder() : UPLOADS.get(player.getUUID());
        if (sb == null) {
            return;
        }
        sb.append(save.chunk());
        if (sb.length() > 4_000_000) {
            UPLOADS.remove(player.getUUID());
            return;
        }
        if (save.part() + 1 < save.parts()) {
            UPLOADS.put(player.getUUID(), sb);
            return;
        }
        UPLOADS.remove(player.getUUID());
        try {
            MetierDefs.replace(MetierDefs.fromJson(sb.toString()));
            player.displayClientMessage(Component.translatable("metier.arcanes_oublies.saved", MetierDefs.all().size()).withStyle(ChatFormatting.GREEN), false);
            ArcanesOublies.LOGGER.info("Trades: {} saved {} trade(s)", player.getGameProfile().getName(), MetierDefs.all().size());
            PacketDistributor.sendToPlayer(player, new MetierNet.Editor(MetierDefs.toJson(MetierDefs.get())));
        } catch (RuntimeException e) {
            player.displayClientMessage(Component.translatable("metier.arcanes_oublies.save_error", e.getMessage()).withStyle(ChatFormatting.RED), false);
        }
    }

    // ---------------------------------------------------------------- for tests

    /** Starts the minigame of {@code def} on {@code pos} for {@code player} as a right click would (tests). */
    public static boolean startForTest(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        Def def = MetierDefs.forBlock(level.getBlockState(pos));
        if (def == null || def.isStation()) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        Tool tool = def.tool(held);
        if (!def.tools.isEmpty() && tool == null) {
            return false;
        }
        begin(player, level, pos, level.getBlockState(pos), def, -1, tool == null ? null : held.getItem(), def.difficulty, ItemStack.EMPTY, ItemStack.EMPTY,
                List.of(), def.name, "");
        return true;
    }

    /** Chooses recipe {@code recipe} of the station {@code def} at {@code pos}, as the recipe list would (tests). */
    public static void pickForTest(ServerPlayer player, String def, BlockPos pos, int recipe) {
        pick(player, new MetierNet.Pick(def, pos, recipe));
    }

    /** The session going on for {@code player}, or -1. */
    /** Right-clicks a grindstone with what the player holds (tests); true if its minigame started. */
    public static boolean grindForTest(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        if (!Grindstone.wants(player, state)) {
            return false;
        }
        Grindstone.start(player, level, pos, state);
        return SESSIONS.containsKey(player.getUUID());
    }

    public static int sessionOf(ServerPlayer player) {
        Session s = SESSIONS.get(player.getUUID());
        return s == null ? -1 : s.id();
    }

    /** Settles {@code player}'s minigame as if it had lasted long enough (tests). */
    public static void finishForTest(ServerPlayer player, float score, boolean critical) {
        Session s = SESSIONS.get(player.getUUID());
        if (s != null) {
            SESSIONS.put(player.getUUID(), new Session(s.id(), s.player(), s.def(), s.recipe(), s.level(), s.pos(), s.state(), s.tool(),
                    s.started() - MIN_MILLIS - 1, s.difficulty()));
            result(player, new MetierNet.Result(s.id(), score, critical));
        }
    }

    public static Game gameOf(Def def) {
        return def.game;
    }
}
