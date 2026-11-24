package com.waylays.arcanes.piege;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.rp.arms.Proc;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The traps: twelve blocks (see {@link TrapKind}) that hurt whoever steps in them (or passes in front of them), and
 * /piege, with which a game master sets, on the trap they hold, its damage and the effects it may give, each with
 * its chance (any harmful effect of the game or of a mod, or setting alight, freezing, lightning, throwing back).
 * The setup stays on the trap once placed, and comes back with it when it is broken.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class Pieges {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ArcanesOublies.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ArcanesOublies.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ArcanesOublies.MODID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ArcanesOublies.MODID);

    /** What /piege set on a trap (on the item, and on the trap once placed). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TrapSetup>> SETUP = COMPONENTS.register("piege",
            () -> DataComponentType.<TrapSetup>builder().persistent(TrapSetup.CODEC).networkSynchronized(TrapSetup.STREAM).build());

    public static final Map<TrapKind, DeferredHolder<Block, TrapBlock>> TRAPS = new EnumMap<>(TrapKind.class);
    public static final Map<TrapKind, DeferredHolder<Item, BlockItem>> TRAP_ITEMS = new EnumMap<>(TrapKind.class);

    static {
        for (TrapKind kind : TrapKind.values()) {
            DeferredHolder<Block, TrapBlock> block = BLOCKS.register(kind.id, () -> new TrapBlock(kind, properties(kind)));
            TRAPS.put(kind, block);
            TRAP_ITEMS.put(kind, ITEMS.register(kind.id, () -> new BlockItem(block.get(), new Item.Properties())));
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrapBlockEntity>> TYPE = BLOCK_ENTITIES.register("piege",
            () -> BlockEntityType.Builder.of(TrapBlockEntity::new, TRAPS.values().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));

    /** The traps' damage ("... was caught in a trap"). */
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ArcanesOublies.id("trap"));
    /** On a dart: the setup of the trap that shot it. */
    static final String DART = "arcanes_piege";
    /** The workshop's own actions a trap can give. */
    public static final List<String> SPECIALS = List.of("*feu", "*gel", "*foudre", "*recul");
    private static final Random RANDOM = new Random();

    private Pieges() {}

    private static BlockBehaviour.Properties properties(TrapKind kind) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().strength(1.5f, 6);
        p = switch (kind) {
            case NAILS, SNARE -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1f, 2).sound(SoundType.WOOD);
            case TAR -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(.6f).sound(SoundType.HONEY_BLOCK);
            case FIRE, GAS -> p.mapColor(MapColor.STONE).sound(SoundType.STONE);
            case DARTS -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5f, 6).sound(SoundType.STONE);
            default -> p.mapColor(MapColor.METAL).sound(SoundType.METAL);
        };
        if (kind == TrapKind.FIRE) {
            p = p.lightLevel(state -> state.getValue(TrapBlock.ACTIVE) ? 13 : 3);
        }
        return kind == TrapKind.DARTS ? p : p.noOcclusion().noCollission();
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        COMPONENTS.register(bus);
    }

    /** The trap kind of {@code stack}, if it is a trap. */
    public static TrapKind kindOf(ItemStack stack) {
        return stack.getItem() instanceof BlockItem b && b.getBlock() instanceof TrapBlock t ? t.kind : null;
    }

    // ---------------------------------------------------------------- what a trap does to whoever it catches

    /** Whether a trap leaves {@code e} alone: the dead, spectators and players in creative mode. */
    public static boolean ignored(Entity e) {
        return !(e instanceof LivingEntity) || !e.isAlive() || e.isSpectator() || e instanceof Player p && p.isCreative();
    }

    /** The trap at {@code from} strikes {@code e}: {@code damage}, then each effect set on it, if its chance comes up. */
    public static void strike(ServerLevel level, Vec3 from, TrapSetup setup, LivingEntity e, float damage) {
        if (damage > 0) {
            e.hurt(level.damageSources().source(DAMAGE), damage);
        }
        effects(level, from, setup, e);
    }

    /** Each effect of {@code setup}, rolled against its chance, on {@code e}. */
    public static void effects(ServerLevel level, Vec3 from, TrapSetup setup, LivingEntity e) {
        for (Proc raw : setup.effects()) {
            Proc p = raw.normalized();
            if (RANDOM.nextFloat() * 100 >= p.chance()) {
                continue;
            }
            int secs = Math.max(1, p.seconds()), power = Math.max(1, p.power());
            switch (p.type()) {
                case "*feu" -> e.igniteForSeconds(secs);
                case "*gel" -> {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, secs * 20, power));
                    e.setTicksFrozen(Math.max(e.getTicksFrozen(), 140 + secs * 20));
                }
                case "*foudre" -> {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                    if (bolt != null) {
                        bolt.moveTo(e.getX(), e.getY(), e.getZ());
                        bolt.setVisualOnly(true);
                        level.addFreshEntity(bolt);
                    }
                    e.hurt(level.damageSources().lightningBolt(), power * 2);
                }
                case "*recul" -> {
                    Vec3 d = e.position().subtract(from).multiply(1, 0, 1);
                    d = d.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : d.normalize();
                    e.knockback(.5 + power * .4, -d.x, -d.z);
                    e.hurtMarked = true;
                }
                default -> {
                    Holder<MobEffect> effect = effect(p.type());
                    if (effect != null) {
                        e.addEffect(new MobEffectInstance(effect, secs * 20, power - 1));
                    }
                }
            }
        }
    }

    public static Holder<MobEffect> effect(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
    }

    /** Whether {@code type} is an effect a trap can give. */
    public static boolean known(String type) {
        return SPECIALS.contains(type) || effect(type) != null;
    }

    /** A dart a dart trap shot hits: the trap's effects. */
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow dart) || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag data = dart.getPersistentData();
        if (!data.contains(DART)) {
            return;
        }
        TrapSetup setup = TrapSetup.CODEC.parse(NbtOps.INSTANCE, data.get(DART)).result().orElse(TrapSetup.NONE);
        data.remove(DART);
        effects(level, dart.position(), setup, event.getEntity());
    }

    static Tag encode(TrapSetup setup) {
        return TrapSetup.CODEC.encodeStart(NbtOps.INSTANCE, setup).result().orElseGet(CompoundTag::new);
    }

    // ---------------------------------------------------------------- /piege

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), event.getBuildContext());
    }

    private static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext context) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("piege").requires(s -> s.hasPermission(2)).executes(Pieges::open)
                .then(Commands.literal("info").executes(Pieges::info))
                .then(Commands.literal("degats").then(Commands.argument("degats", FloatArgumentType.floatArg(0, 200))
                        .executes(c -> damage(c, FloatArgumentType.getFloat(c, "degats")))))
                .then(Commands.literal("defaut").executes(c -> damage(c, -1)))
                .then(Commands.literal("effet").then(Commands.argument("effet", ResourceArgument.resource(context, Registries.MOB_EFFECT))
                        .executes(c -> add(c, ResourceArgument.getMobEffect(c, "effet").getRegisteredName(), 25, 1, 5))
                        .then(Commands.argument("chance", FloatArgumentType.floatArg(1, 100))
                                .executes(c -> add(c, ResourceArgument.getMobEffect(c, "effet").getRegisteredName(), FloatArgumentType.getFloat(c, "chance"), 1, 5))
                                .then(Commands.argument("niveau", IntegerArgumentType.integer(1, 255))
                                        .executes(c -> add(c, ResourceArgument.getMobEffect(c, "effet").getRegisteredName(), FloatArgumentType.getFloat(c, "chance"),
                                                IntegerArgumentType.getInteger(c, "niveau"), 5))
                                        .then(Commands.argument("secondes", IntegerArgumentType.integer(1, 3600))
                                                .executes(c -> add(c, ResourceArgument.getMobEffect(c, "effet").getRegisteredName(),
                                                        FloatArgumentType.getFloat(c, "chance"), IntegerArgumentType.getInteger(c, "niveau"),
                                                        IntegerArgumentType.getInteger(c, "secondes"))))))))
                .then(Commands.literal("special").then(Commands.argument("action", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SPECIALS.stream().map(s -> s.substring(1)), b))
                        .executes(c -> add(c, "*" + StringArgumentType.getString(c, "action"), 25, 1, 4))
                        .then(Commands.argument("chance", FloatArgumentType.floatArg(1, 100))
                                .executes(c -> add(c, "*" + StringArgumentType.getString(c, "action"), FloatArgumentType.getFloat(c, "chance"), 1, 4))
                                .then(Commands.argument("puissance", IntegerArgumentType.integer(1, 255))
                                        .then(Commands.argument("secondes", IntegerArgumentType.integer(1, 3600))
                                                .executes(c -> add(c, "*" + StringArgumentType.getString(c, "action"), FloatArgumentType.getFloat(c, "chance"),
                                                        IntegerArgumentType.getInteger(c, "puissance"), IntegerArgumentType.getInteger(c, "secondes"))))))))
                .then(Commands.literal("vider").executes(Pieges::clear));
        LiteralCommandNode<CommandSourceStack> node = d.register(root);
        // The same with its accent
        d.register(Commands.literal("piège").requires(s -> s.hasPermission(2)).executes(Pieges::open).redirect(node));
    }

    /** The trap the command's player holds, or null (and a message). */
    private static ItemStack held(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ItemStack stack = c.getSource().getPlayerOrException().getMainHandItem();
        if (kindOf(stack) == null) {
            c.getSource().sendFailure(Component.translatable("gui.arcanes_oublies.piege.hold"));
            return null;
        }
        return stack;
    }

    private static TrapSetup setupOf(ItemStack stack) {
        return stack.getOrDefault(SETUP.get(), TrapSetup.NONE);
    }

    private static void set(ItemStack stack, TrapSetup setup) {
        setup = setup.sane();
        if (setup.equals(TrapSetup.NONE)) {
            stack.remove(SETUP.get());
        } else {
            stack.set(SETUP.get(), setup);
        }
    }

    private static int open(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        if (held(c) == null) {
            return 0;
        }
        PacketDistributor.sendToPlayer(c.getSource().getPlayerOrException(), new Open());
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ItemStack stack = held(c);
        if (stack == null) {
            return 0;
        }
        TrapKind kind = kindOf(stack);
        TrapSetup s = setupOf(stack);
        c.getSource().sendSuccess(() -> Component.translatable(s.damage() < 0 ? "gui.arcanes_oublies.piege.info.default" : "gui.arcanes_oublies.piege.info.damage",
                number(s.damageFor(kind))).withStyle(ChatFormatting.GOLD), false);
        if (s.effects().isEmpty()) {
            c.getSource().sendSuccess(() -> Component.translatable("gui.arcanes_oublies.piege.info.none").withStyle(ChatFormatting.GRAY), false);
        }
        for (Proc p : s.effects()) {
            c.getSource().sendSuccess(() -> line(p), false);
        }
        return 1;
    }

    private static int damage(CommandContext<CommandSourceStack> c, float damage) throws CommandSyntaxException {
        ItemStack stack = held(c);
        if (stack == null) {
            return 0;
        }
        set(stack, new TrapSetup(damage, setupOf(stack).effects()));
        TrapKind kind = kindOf(stack);
        c.getSource().sendSuccess(() -> Component.translatable("gui.arcanes_oublies.piege.damage_set", number(setupOf(stack).damageFor(kind))), false);
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> c, String type, float chance, int power, int seconds) throws CommandSyntaxException {
        ItemStack stack = held(c);
        if (stack == null) {
            return 0;
        }
        if (!known(type)) {
            c.getSource().sendFailure(Component.translatable("gui.arcanes_oublies.piege.unknown", type));
            return 0;
        }
        TrapSetup s = setupOf(stack);
        if (s.effects().size() >= TrapSetup.MAX_EFFECTS) {
            c.getSource().sendFailure(Component.translatable("gui.arcanes_oublies.piege.full", TrapSetup.MAX_EFFECTS));
            return 0;
        }
        List<Proc> effects = new ArrayList<>(s.effects());
        Proc p = new Proc(type, chance, power, seconds, false, false);
        effects.add(p);
        set(stack, new TrapSetup(s.damage(), effects));
        c.getSource().sendSuccess(() -> Component.translatable("gui.arcanes_oublies.piege.added", line(p)), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ItemStack stack = held(c);
        if (stack == null) {
            return 0;
        }
        set(stack, new TrapSetup(setupOf(stack).damage(), List.of()));
        c.getSource().sendSuccess(() -> Component.translatable("gui.arcanes_oublies.piege.cleared"), false);
        return 1;
    }

    /** "✦ 25 % : Poison II (5 s)". */
    public static Component line(Proc raw) {
        Proc p = raw.normalized();
        Component what;
        if (p.special()) {
            what = Component.translatable("arms.arcanes_oublies.special." + p.type().substring(1));
        } else {
            Holder<MobEffect> e = effect(p.type());
            what = e == null ? Component.literal(p.type()) : Component.translatable(e.value().getDescriptionId());
        }
        String level = p.power() > 1 ? " " + roman(p.power()) : "";
        return Component.translatable("gui.arcanes_oublies.piege.line", number(p.chance()), what, level, p.seconds()).withColor(0xFFFF9A7A);
    }

    public static String number(float v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    // ---------------------------------------------------------------- the screen's packets

    public static void registerPayloads(PayloadRegistrar r) {
        r.playToClient(Open.TYPE, Open.CODEC, (o, c) -> c.enqueueWork(com.waylays.arcanes.piege.client.TrapScreen::open));
        r.playToServer(Edit.TYPE, Edit.CODEC, (e, c) -> c.enqueueWork(() -> edit(e, c)));
    }

    private static void edit(Edit e, IPayloadContext c) {
        if (!(c.player() instanceof ServerPlayer p) || !p.hasPermissions(2)) {
            return;
        }
        ItemStack stack = p.getMainHandItem();
        if (kindOf(stack) == null) {
            return;
        }
        List<Proc> kept = e.setup().effects().stream().filter(x -> known(x.type())).toList();
        set(stack, new TrapSetup(e.setup().damage(), kept));
        p.displayClientMessage(Component.translatable("gui.arcanes_oublies.piege.saved").withStyle(ChatFormatting.GOLD), true);
    }

    /** Opens the trap's screen (on the trap in hand). */
    public record Open() implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ArcanesOublies.id("piege_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.unit(new Open());

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    /** What the screen set on the trap in hand. */
    public record Edit(TrapSetup setup) implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(ArcanesOublies.id("piege_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Edit> CODEC = TrapSetup.STREAM.map(Edit::new, Edit::setup);

        @Override
        public Type<Edit> type() {
            return TYPE;
        }
    }
}
