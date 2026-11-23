package com.waylays.arcanes.metier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * The grindstone: a right click on it with a worn weapon (or any item with durability) starts its sharpening
 * minigame ({@code GrindGame}). A critical success gives back 200 durability, a success 100, a failure takes 50.
 * Each weapon may go back to the grindstone only 12 hours (real time) later. Game masters keep some weapons from it
 * altogether: the kind of item (in /arcanes metiers, "Meule", or /meule interdire [id]: every iron dagger), or an item
 * as it is made (the editor's "exact items", or /meule interdire objet: this dagger of 8 damage and every one made the
 * same, but not the other daggers).
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class Grindstone extends SavedData {
    /** The minigame sessions' trade id. */
    static final String ID = "arcanes:meule";
    private static final String NAME = ArcanesOublies.MODID + "_meule";
    /** On the item: when it may go back to the grindstone (ms since 1970), and whether it is kept from it. */
    private static final String NEXT = "arcanes_meule_suivante", BANNED = "arcanes_meule_interdite";
    private static final long COOLDOWN = 12L * 3600 * 1000;
    private static final int CRITICAL = 200, SUCCESS = 100, FAILURE = 50;
    private static final float PERFECT_AT = .82f, CRITICAL_BELOW = .3f;

    /** The kinds of item kept from the grindstone by an older version (the list is now in metiers.json, see {@link #bannedKinds}). */
    private final Set<String> banned = new TreeSet<>();

    private static Grindstone get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Grindstone::new, Grindstone::load, null), NAME);
    }

    private static Grindstone load(CompoundTag tag, HolderLookup.Provider registries) {
        Grindstone g = new Grindstone();
        for (Tag t : tag.getList("interdits", Tag.TAG_STRING)) {
            g.banned.add(t.getAsString());
        }
        return g;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        banned.forEach(s -> list.add(StringTag.valueOf(s)));
        tag.put("interdits", list);
        return tag;
    }

    // ---------------------------------------------------------------- the minigame

    /** Whether a right click on this block, with what the player holds, is for the grindstone's minigame. */
    static boolean wants(ServerPlayer player, BlockState state) {
        return state.is(Blocks.GRINDSTONE) && player.getMainHandItem().isDamageableItem();
    }

    private static CompoundTag data(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    static boolean banned(MinecraftServer server, ItemStack stack) {
        return data(stack).getBoolean(BANNED) || bannedKinds(server).contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                || bannedExactly(server, stack);
    }

    // ---------------------------------------------------------------- single items, as they are made

    /** What wear and use change on an item: its signature leaves them out (a worn, renamed or enchanted dagger is the same dagger). */
    private static final List<DataComponentType<?>> WEAR = List.of(DataComponents.DAMAGE, DataComponents.REPAIR_COST, DataComponents.ENCHANTMENTS,
            DataComponents.CUSTOM_NAME, DataComponents.TRIM, DataComponents.DYED_COLOR);

    /** Puts {@code type} back as the item's kind has it. */
    private static <T> void reset(ItemStack stack, DataComponentType<T> type) {
        stack.set(type, stack.getItem().components().get(type));
    }

    /**
     * How {@code stack} is made: its kind, and its components but those wear and use change (its damage, enchantments,
     * name, trim, dye, and the grindstone's own marks), as SNBT; with its name and stats for the editor.
     */
    public static MetierDefs.GrindItem signature(HolderLookup.Provider registries, ItemStack stack) {
        ItemStack bare = stack.copyWithCount(1);
        WEAR.forEach(type -> reset(bare, type));
        CompoundTag custom = data(bare);
        custom.remove(NEXT);
        custom.remove(BANNED);
        if (custom.isEmpty()) {
            reset(bare, DataComponents.CUSTOM_DATA);
        } else {
            bare.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        }
        Tag made = DataComponentPatch.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), bare.getComponentsPatch()).result()
                .orElseGet(CompoundTag::new);
        MetierDefs.GrindItem g = new MetierDefs.GrindItem();
        g.item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        g.components = made.toString();
        g.name = stack.getHoverName().getString();
        // Its stats as its tooltip shows them (a hand's base damage 1 and speed 4 added)
        double damage = 0, speed = 0, armor = 0, toughness = 0;
        boolean weapon = false, armour = false;
        for (ItemAttributeModifiers.Entry e : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
            if (e.modifier().operation() != AttributeModifier.Operation.ADD_VALUE) {
                continue;
            }
            double v = e.modifier().amount();
            if (e.attribute().value() == Attributes.ATTACK_DAMAGE.value()) {
                damage += v;
                weapon = true;
            } else if (e.attribute().value() == Attributes.ATTACK_SPEED.value()) {
                speed += v;
                weapon = true;
            } else if (e.attribute().value() == Attributes.ARMOR.value()) {
                armor += v;
                armour = true;
            } else if (e.attribute().value() == Attributes.ARMOR_TOUGHNESS.value()) {
                toughness += v;
                armour = true;
            }
        }
        if (weapon) {
            g.damage = 1 + damage;
            g.speed = 4 + speed;
        }
        if (armour) {
            g.armor = armor;
            g.toughness = toughness;
        }
        return g;
    }

    @Nullable
    private static Tag parse(String snbt) {
        try {
            return TagParser.parseTag(snbt);
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    /** Whether {@code a} and {@code b} are the same item made the same way (their components compared as tags, not as text). */
    private static boolean same(MetierDefs.GrindItem a, MetierDefs.GrindItem b) {
        if (!a.item.equals(b.item)) {
            return false;
        }
        Tag ta = parse(a.components);
        return ta != null && Objects.equals(ta, parse(b.components));
    }

    /** Whether {@code stack} is made as one of the single items kept from the grindstone. */
    public static boolean bannedExactly(MinecraftServer server, ItemStack stack) {
        List<MetierDefs.GrindItem> list = MetierDefs.get().grindstoneExact;
        if (list.isEmpty()) {
            return false;
        }
        MetierDefs.GrindItem mine = signature(server.registryAccess(), stack);
        for (MetierDefs.GrindItem g : list) {
            if (same(g, mine)) {
                return true;
            }
        }
        return false;
    }

    /** Keeps the items made as {@code stack} from the grindstone, or lets them back; saved in the trades' file. */
    public static void setBannedExactly(MinecraftServer server, ItemStack stack, boolean ban) {
        MetierDefs.GrindItem mine = signature(server.registryAccess(), stack);
        List<MetierDefs.GrindItem> list = MetierDefs.get().grindstoneExact;
        list.removeIf(g -> same(g, mine));
        if (ban) {
            list.add(mine);
        }
        MetierDefs.save();
    }

    /**
     * The kinds of item kept from the grindstone: the list in the trades' file (edited in /arcanes metiers or with
     * /meule), and any an older version kept in the world.
     */
    public static Set<String> bannedKinds(MinecraftServer server) {
        Set<String> all = new TreeSet<>(MetierDefs.get().grindstoneBanned);
        all.addAll(get(server).banned);
        return all;
    }

    /** Keeps the kind {@code id} from the grindstone, or lets it back; saved in the trades' file. */
    public static void setBanned(MinecraftServer server, String id, boolean ban) {
        List<String> list = MetierDefs.get().grindstoneBanned;
        if (ban && !list.contains(id)) {
            list.add(id);
        } else if (!ban) {
            list.remove(id);
            Grindstone old = get(server);
            if (old.banned.remove(id)) {
                old.setDirty();
            }
        }
        MetierDefs.save();
    }

    static void start(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack held = player.getMainHandItem();
        if (banned(level.getServer(), held)) {
            rp(player, Component.translatable("metier.arcanes_oublies.grind.banned"));
            return;
        }
        if (held.getDamageValue() <= 0) {
            rp(player, Component.translatable("metier.arcanes_oublies.grind.whole"));
            return;
        }
        long left = data(held).getLong(NEXT) - System.currentTimeMillis();
        if (left > 0) {
            rp(player, Component.translatable("metier.arcanes_oublies.grind.rest", MetierService.time(left / 1000)));
            return;
        }
        MetierService.begin(player, level, pos, state, ID, MetierDefs.Game.GRIND, -1, held.getItem(), 1, held.copyWithCount(1), ItemStack.EMPTY,
                List.of(), Component.translatable("metier.arcanes_oublies.grind.title").getString(), held.getHoverName().getString(), PERFECT_AT,
                CRITICAL_BELOW);
    }

    /** The minigame's end: the weapon's durability, its next turn at the grindstone, a line in character. */
    static void settle(ServerPlayer player, int session, ServerLevel level, BlockPos pos, BlockState state, @Nullable Item weapon, float score,
                       boolean critical, boolean tooFast) {
        ItemStack held = player.getMainHandItem();
        if (player.level() != level || player.distanceToSqr(pos.getCenter()) > MetierService.MAX_DISTANCE * MetierService.MAX_DISTANCE
                || !level.getBlockState(pos).is(state.getBlock())) {
            PacketDistributor.sendToPlayer(player, new MetierNet.Stop(session, "metier.arcanes_oublies.stop.gone"));
            return;
        }
        if (weapon == null || !held.is(weapon) || !held.isDamageableItem()) {
            PacketDistributor.sendToPlayer(player, new MetierNet.Stop(session, "metier.arcanes_oublies.stop.tool"));
            return;
        }
        score = Math.max(0, Math.min(1, score));
        MetierDefs.Outcome outcome = critical || tooFast || score < CRITICAL_BELOW ? MetierDefs.Outcome.CRITICAL
                : score >= PERFECT_AT ? MetierDefs.Outcome.PERFECT : MetierDefs.Outcome.PARTIAL;
        int change = switch (outcome) {
            case PERFECT -> CRITICAL;
            case PARTIAL -> SUCCESS;
            case CRITICAL -> -FAILURE;
        };
        int max = held.getMaxDamage();
        // A failure wears the blade but never breaks it
        held.setDamageValue(Math.max(0, Math.min(max - 1, held.getDamageValue() - change)));
        CustomData.update(DataComponents.CUSTOM_DATA, held, t -> t.putLong(NEXT, System.currentTimeMillis() + COOLDOWN));
        level.playSound(null, pos, switch (outcome) {
            case PERFECT -> SoundEvents.ANVIL_USE;
            case PARTIAL -> SoundEvents.GRINDSTONE_USE;
            case CRITICAL -> SoundEvents.ITEM_BREAK;
        }, SoundSource.PLAYERS, .7f, outcome == MetierDefs.Outcome.PERFECT ? 1.5f : 1f);
        rp(player, Component.translatable(switch (outcome) {
            case PERFECT -> "metier.arcanes_oublies.grind.perfect";
            case PARTIAL -> "metier.arcanes_oublies.grind.done";
            case CRITICAL -> "metier.arcanes_oublies.grind.failed";
        }, held.getHoverName(), Math.abs(change)));
        // The result screen: durability given back (negative wear), or lost
        PacketDistributor.sendToPlayer(player, new MetierNet.Outcome(session, outcome.ordinal(), List.of(), -change));
    }

    /** A line in character, in the chat. */
    private static void rp(ServerPlayer player, Component line) {
        player.sendSystemMessage(Component.literal("✦ ").withStyle(ChatFormatting.DARK_GRAY).append(line.copy().withStyle(ChatFormatting.GRAY,
                ChatFormatting.ITALIC)));
    }

    // ---------------------------------------------------------------- the game masters' list

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), event.getBuildContext());
    }

    private static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext context) {
        d.register(Commands.literal("meule").requires(s -> s.hasPermission(RpConfig.get(RpConfig.REPAIR_PERMISSION, 2)))
                .then(Commands.literal("interdire").executes(c -> kind(c, true))
                        .then(Commands.literal("objet").executes(c -> one(c, true)))
                        .then(Commands.argument("id", ItemArgument.item(context)).executes(c -> kind(c, ItemArgument.getItem(c, "id").getItem(), true))))
                .then(Commands.literal("autoriser").executes(c -> kind(c, false))
                        .then(Commands.literal("objet").executes(c -> one(c, false)))
                        .then(Commands.argument("id", ItemArgument.item(context)).executes(c -> kind(c, ItemArgument.getItem(c, "id").getItem(), false))))
                .then(Commands.literal("liste").executes(Grindstone::list))
                .then(Commands.literal("reset").executes(c -> {
                    ItemStack held = held(c);
                    if (held == null) {
                        return 0;
                    }
                    CustomData.update(DataComponents.CUSTOM_DATA, held, t -> t.remove(NEXT));
                    c.getSource().sendSuccess(() -> Component.translatable("metier.arcanes_oublies.grind.reset", held.getHoverName()), true);
                    return 1;
                })));
    }

    @Nullable
    private static ItemStack held(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ItemStack held = c.getSource().getPlayerOrException().getMainHandItem();
        if (held.isEmpty()) {
            c.getSource().sendFailure(Component.translatable("metier.arcanes_oublies.grind.hold"));
            return null;
        }
        return held;
    }

    /** Keeps (or lets back) every item of the held one's kind from the grindstone. */
    private static int kind(CommandContext<CommandSourceStack> c, boolean ban) throws CommandSyntaxException {
        ItemStack held = held(c);
        return held == null ? 0 : kind(c, held.getItem(), ban);
    }

    /** Keeps (or lets back) every item of this kind from the grindstone. */
    private static int kind(CommandContext<CommandSourceStack> c, Item item, boolean ban) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        setBanned(c.getSource().getServer(), id.toString(), ban);
        c.getSource().sendSuccess(() -> Component.translatable(ban ? "metier.arcanes_oublies.grind.banned_kind" : "metier.arcanes_oublies.grind.allowed_kind",
                item.getDescription()), true);
        return 1;
    }

    /** Keeps (or lets back) the held item as it is made from the grindstone: it, and any made the same, but not its whole kind. */
    private static int one(CommandContext<CommandSourceStack> c, boolean ban) throws CommandSyntaxException {
        ItemStack held = held(c);
        if (held == null) {
            return 0;
        }
        // The mark an older version put on the one item goes; the list in the trades' file is what counts now
        if (!ban || data(held).getBoolean(BANNED)) {
            CustomData.update(DataComponents.CUSTOM_DATA, held, t -> t.remove(BANNED));
        }
        setBannedExactly(c.getSource().getServer(), held, ban);
        c.getSource().sendSuccess(() -> Component.translatable(ban ? "metier.arcanes_oublies.grind.banned_one" : "metier.arcanes_oublies.grind.allowed_one",
                held.getHoverName()), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        Set<String> all = bannedKinds(c.getSource().getServer());
        List<String> exact = MetierDefs.get().grindstoneExact.stream().map(g -> g.name.isBlank() ? g.item : g.name).toList();
        c.getSource().sendSuccess(() -> Component.translatable("metier.arcanes_oublies.grind.list", all.isEmpty() ? "—" : String.join(", ", all)), false);
        c.getSource().sendSuccess(() -> Component.translatable("metier.arcanes_oublies.grind.list_exact", exact.isEmpty() ? "—" : String.join(", ", exact)),
                false);
        return all.size() + exact.size();
    }
}
