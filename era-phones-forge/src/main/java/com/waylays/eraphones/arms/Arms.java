package com.waylays.eraphones.arms;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.registry.EraRegistry;
import com.waylays.eraphones.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import com.waylays.eraphones.compat.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import com.waylays.eraphones.compat.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import com.waylays.eraphones.compat.ByteBufCodecs;
import com.waylays.eraphones.compat.StreamCodec;
import com.waylays.eraphones.compat.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import com.waylays.eraphones.compat.PacketDistributor;
import com.waylays.eraphones.compat.IPayloadContext;
import com.waylays.eraphones.compat.PayloadRegistrar;
import com.waylays.eraphones.compat.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The weapons' workshop, for game masters: /arme opens it on the item in hand (its name, its lore, its stats, whether
 * it is unbreakable and shines, and its chance effects). The chance effects themselves: on a weapon, rolled when it
 * strikes; on armour (or anything held), when its bearer is struck.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class Arms {
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Proc>>> PROCS = EraRegistry.COMPONENTS.register("effets_chance",
            () -> DataComponentType.<List<Proc>>builder().persistent(Proc.LIST_CODEC).networkSynchronized(Proc.LIST_STREAM).build());
    /** The stats the workshop sets, in its order. */
    public static final List<Holder<Attribute>> STATS = List.of(Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED, Attributes.ARMOR,
            Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE, Attributes.MOVEMENT_SPEED, Attributes.MAX_HEALTH,
            Attributes.ENTITY_INTERACTION_RANGE);
    /** What the workshop shows a stat as, against the modifier: the hand's own attack damage (1) and speed (4) added. */
    public static final double[] SHOWN_BASE = {1, 4, 0, 0, 0, 0, 0, 0};
    private static final Random RANDOM = new Random();
    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);

    private Arms() {}

    /** Loads the class (so its data component is registered with the others). */
    public static void init() {
    }

    public static EquipmentSlotGroup group(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof Equipable e && e.getEquipmentSlot().isArmor()) {
            return EquipmentSlotGroup.bySlot(e.getEquipmentSlot());
        }
        return EquipmentSlotGroup.MAINHAND;
    }

    private static ResourceLocation idFor(int i, EquipmentSlotGroup g) {
        // The hand's own damage and speed keep the game's ids: shown as the weapon's base, in green
        if (g == EquipmentSlotGroup.MAINHAND && i == 0) {
            return new ResourceLocation("base_attack_damage");
        }
        if (g == EquipmentSlotGroup.MAINHAND && i == 1) {
            return new ResourceLocation("base_attack_speed");
        }
        return EraPhones.id("atelier_" + i + "_" + g.getSerializedName());
    }

    /** The stats as the workshop shows them (NaN: not set). */
    public static double[] stats(ItemStack stack) {
        double[] out = new double[STATS.size()];
        java.util.Arrays.fill(out, Double.NaN);
        ItemAttributeModifiers mods = stack.getAttributeModifiers();
        for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
            int i = STATS.indexOf(e.attribute());
            if (i < 0) {
                continue;
            }
            double v = e.modifier().amount();
            if (i == 5 && e.modifier().operation() != AttributeModifier.Operation.ADD_VALUE) {
                v *= 100;
            }
            out[i] = (Double.isNaN(out[i]) ? SHOWN_BASE[i] : out[i]) + v;
        }
        return out;
    }

    // ---------------------------------------------------------------- the workshop

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("arme").requires(s -> s.hasPermission(RpConfig.get(RpConfig.HORROR_PERMISSION, 2)))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (p.getMainHandItem().isEmpty()) {
                        c.getSource().sendFailure(Component.translatable("arms.eraphones.hold_something"));
                        return 0;
                    }
                    PacketDistributor.sendToPlayer(p, new Open());
                    return 1;
                }));
    }

    public static void register(PayloadRegistrar r) {
        r.playToClient(Open.TYPE, Open.CODEC, (o, c) -> c.enqueueWork(com.waylays.eraphones.arms.client.WorkshopScreen::open));
        r.playToServer(Edit.TYPE, Edit.CODEC, (e, c) -> c.enqueueWork(() -> apply(e, c)));
    }

    private static void apply(Edit e, IPayloadContext c) {
        if (!(c.player() instanceof ServerPlayer p) || !p.hasPermissions(RpConfig.get(RpConfig.HORROR_PERMISSION, 2))) {
            return;
        }
        ItemStack stack = p.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }
        if (e.name().isBlank()) {
            stack.remove(DataComponents.CUSTOM_NAME);
        } else {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(colours(e.name())).withStyle(s -> s.withItalic(false)));
        }
        List<Component> lore = new ArrayList<>();
        // Blank lines are kept: they space the lore out
        for (String l : e.lore()) {
            lore.add(Component.literal(colours(l)).withStyle(s -> s.withItalic(false).withColor(ChatFormatting.GRAY)));
        }
        if (lore.isEmpty()) {
            stack.remove(DataComponents.LORE);
        } else {
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        if (e.unbreakable()) {
            stack.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        } else {
            stack.remove(DataComponents.UNBREAKABLE);
        }
        if (e.glint()) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
        // The stats: set ones replace the item's, unset ones are left out
        EquipmentSlotGroup g = group(stack);
        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        for (int i = 0; i < STATS.size() && i < e.stats().size(); i++) {
            float v = e.stats().get(i);
            if (Float.isNaN(v)) {
                continue;
            }
            double amount = v - SHOWN_BASE[i];
            AttributeModifier.Operation op = AttributeModifier.Operation.ADD_VALUE;
            if (i == 5) {
                amount = v / 100.0;
                op = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            }
            b.add(STATS.get(i), new AttributeModifier(idFor(i, g), amount, op), g);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());
        List<Proc> procs = new ArrayList<>();
        for (Proc pr : e.procs()) {
            if (known(pr.type()) && pr.chance() > 0) {
                procs.add(new Proc(pr.type(), Math.min(100, pr.chance()), Math.max(0, Math.min(255, pr.power())), Math.max(0, Math.min(3600, pr.seconds())),
                        pr.guard(), pr.self()));
            }
        }
        if (procs.isEmpty()) {
            PROCS.get().remove(stack);
        } else {
            PROCS.get().set(stack, procs);
        }
        p.displayClientMessage(Component.translatable("arms.eraphones.saved").withStyle(ChatFormatting.GOLD), true);
    }

    /** "&c" and the like turned to colours. */
    private static String colours(String s) {
        return s.replaceAll("&([0-9a-fk-or])", "§$1");
    }

    // ---------------------------------------------------------------- the chance effects

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (BUSY.get() || event.getEntity().level().isClientSide) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim) {
            return;
        }
        BUSY.set(true);
        try {
            // A blow struck: what the striker holds (only for a blow of the hand)
            if (event.getSource().getDirectEntity() == attacker) {
                List<Proc> procs = PROCS.get().get(attacker.getMainHandItem());
                if (procs != null) {
                    for (Proc raw : procs) {
                        Proc p = raw.normalized();
                        if (!p.guard() && RANDOM.nextFloat() * 100 < p.chance()) {
                            fire(p, attacker, victim, event);
                        }
                    }
                }
            }
            // A blow taken: what the one struck wears or holds
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                List<Proc> procs = PROCS.get().get(victim.getItemBySlot(slot));
                if (procs == null) {
                    continue;
                }
                for (Proc raw : procs) {
                    Proc p = raw.normalized();
                    if (p.guard() && RANDOM.nextFloat() * 100 < p.chance()) {
                        fire(p, victim, attacker, event);
                    }
                }
            }
        } finally {
            BUSY.set(false);
        }
    }

    private static void flash(LivingEntity e) {
        if (e.level() instanceof ServerLevel l) {
            l.sendParticles(ParticleTypes.ENCHANTED_HIT, e.getX(), e.getY() + e.getBbHeight() * .6, e.getZ(), 12, .3, .3, .3, .2);
            l.playSound(null, e.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, .6f, 1.4f);
        }
    }

    /** {@code p} happens: {@code me} is its item's bearer, {@code other} the one struck or striking. */
    private static void fire(Proc p, LivingEntity me, LivingEntity other, LivingHurtEvent event) {
        LivingEntity t = p.self() ? me : other;
        int secs = Math.max(0, p.seconds()), power = Math.max(0, p.power());
        if (!p.special()) {
            ResourceLocation id = ResourceLocation.tryParse(p.type());
            Holder<MobEffect> effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
            if (effect == null) {
                return;
            }
            t.addEffect(new MobEffectInstance(effect, Math.max(1, secs * 20), Math.max(0, power - 1)), me);
            flash(t);
            return;
        }
        switch (p.type()) {
            case "*feu" -> t.igniteForSeconds(Math.max(1, secs));
            case "*foudre" -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(t.level());
                if (bolt != null) {
                    bolt.moveTo(t.getX(), t.getY(), t.getZ());
                    bolt.setVisualOnly(true);
                    t.level().addFreshEntity(bolt);
                }
                t.hurt(t.damageSources().lightningBolt(), Math.max(1, power));
            }
            case "*recul" -> {
                LivingEntity from = t == other ? me : other;
                Vec3 d = t.position().subtract(from.position()).multiply(1, 0, 1).normalize();
                t.knockback(.4 + power * .4, -d.x, -d.z);
            }
            case "*vol_de_vie" -> me.heal(event.getAmount() * power / 100f);
            case "*degats" -> event.setAmount(p.guard() ? Math.max(0, event.getAmount() - power) : event.getAmount() + power);
            case "*epines" -> t.hurt(t.damageSources().thorns(me), Math.max(1, power));
            case "*soin" -> t.heal(Math.max(1, power));
            case "*gel" -> {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.max(1, secs) * 20, power), me);
                t.setTicksFrozen(Math.max(t.getTicksFrozen(), 140 + secs * 20));
            }
            default -> {
                return;
            }
        }
        flash(t);
    }

    /** Whether {@code type} is an effect of the game (or a mod) or one of the workshop's own. */
    public static boolean known(String type) {
        if (Proc.SPECIALS.contains(type)) {
            return true;
        }
        ResourceLocation id = ResourceLocation.tryParse(type);
        return id != null && BuiltInRegistries.MOB_EFFECT.containsKey(id);
    }

    // ---------------------------------------------------------------- payloads

    /** Opens the workshop (on the item in hand). */
    public record Open() implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(EraPhones.id("arms_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.unit(new Open());

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    /** What the workshop made of the item in hand (a stat NaN: left unset). */
    public record Edit(String name, List<String> lore, boolean unbreakable, boolean glint, List<Float> stats, List<Proc> procs)
            implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(EraPhones.id("arms_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Edit> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(200), Edit::name, ByteBufCodecs.stringUtf8(300).apply(ByteBufCodecs.list(16)), Edit::lore,
                ByteBufCodecs.BOOL, Edit::unbreakable, ByteBufCodecs.BOOL, Edit::glint, ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list(16)), Edit::stats,
                Proc.LIST_STREAM, Edit::procs, Edit::new);

        @Override
        public Type<Edit> type() {
            return TYPE;
        }
    }
}
