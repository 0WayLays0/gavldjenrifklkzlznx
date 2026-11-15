package com.waylays.arcanes.compat.epicnpc;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * What makes Custom NPCs' NPCs fight like Epic Fight's (only loaded when both mods are there, see {@link EpicNpc}).
 * Custom NPCs keeps deciding who an NPC fights, where it goes and when it strikes, so everything set in its menus
 * still holds; Epic Fight only shows it. When an NPC strikes, its blow is held back, the attack animation of the
 * weapon in its hand plays (one move after the other, as a combo), and the blow lands, as Custom NPCs makes it, when
 * the swing reaches its target. Epic Fight's own attack and chase goals are taken off the NPCs, and the hits of its
 * animations do nothing on them, so that nothing strikes twice.
 */
public final class EpicNpcBridge {
    private static final Set<ResourceLocation> NPCS = Set.of(ResourceLocation.fromNamespaceAndPath("customnpcs", "customnpc"),
            ResourceLocation.fromNamespaceAndPath("customnpcs", "customnpcalex"), ResourceLocation.fromNamespaceAndPath("customnpcs", "customnpcclassic"));
    /** A blow being swung: whom it is for, the tick it lands, the furthest it reaches. */
    private record Pending(LivingEntity target, int landsAt, double reach) {}

    private static final Map<Mob, Pending> PENDING = new WeakHashMap<>();
    private static final Map<Mob, Integer> COMBO = new WeakHashMap<>();
    /** Set while a held-back blow lands, so that it is let through. */
    private static final ThreadLocal<Boolean> LANDING = ThreadLocal.withInitial(() -> false);
    private static boolean warned;

    private EpicNpcBridge() {}

    public static boolean isNpc(Entity e) {
        return e instanceof Mob && NPCS.contains(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()));
    }

    /** The NPC's Epic Fight body, or null (Epic Fight did not give it one). */
    public static LivingEntityPatch<?> patch(Entity e) {
        try {
            return EpicFightCapabilities.getEntityPatch(e, LivingEntityPatch.class);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (attacker == null || attacker.level().isClientSide() || LANDING.get() || !isNpc(attacker)) {
            return;
        }
        Mob npc = (Mob) attacker;
        if (source instanceof EpicFightDamageSource) {
            // The swing of the animation itself: the NPC's own blow lands instead
            event.setCanceled(true);
            return;
        }
        if (source.getDirectEntity() != npc) {
            return;
        }
        LivingEntityPatch<?> patch = patch(npc);
        if (patch == null) {
            return;
        }
        event.setCanceled(true);
        if (PENDING.containsKey(npc)) {
            return;
        }
        try {
            Attack attack = attackFor(npc, patch);
            patch.playAnimationSynchronized(attack.animation(), 0.0F);
            PENDING.put(npc, new Pending(event.getEntity(), npc.tickCount + attack.contact(), attack.reach()));
        } catch (RuntimeException ex) {
            // An animation that could not be played: the blow lands at once, as it would without Epic Fight
            if (!warned) {
                warned = true;
                ArcanesOublies.LOGGER.warn("An NPC's Epic Fight attack could not be played: {}", ex.toString());
            }
            land(npc, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (e.level().isClientSide() || !isNpc(e)) {
            return;
        }
        Mob npc = (Mob) e;
        Pending p = PENDING.get(npc);
        if (p != null && npc.tickCount >= p.landsAt()) {
            PENDING.remove(npc);
            LivingEntity target = p.target();
            if (npc.isAlive() && target.isAlive() && target.level() == npc.level() && npc.distanceTo(target) <= p.reach()) {
                land(npc, target);
            }
        } else if (p != null && npc.tickCount > p.landsAt() + 40) {
            PENDING.remove(npc);
        }
        if (npc.tickCount % 5 == 0) {
            dropEpicFightGoals(npc);
        }
    }

    /** The NPC's blow, as Custom NPCs makes it (its damage, its effects), let through this time. */
    private static void land(Mob npc, LivingEntity target) {
        LANDING.set(true);
        try {
            npc.doHurtTarget(target);
        } finally {
            LANDING.set(false);
        }
    }

    /** Epic Fight's own attack and chase goals: Custom NPCs fights for the NPC. */
    static void dropEpicFightGoals(Mob npc) {
        List<Goal> drop = new ArrayList<>();
        for (WrappedGoal w : npc.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof AnimatedAttackGoal || w.getGoal() instanceof TargetChasingGoal) {
                drop.add(w.getGoal());
            }
        }
        drop.forEach(npc.goalSelector::removeGoal);
    }

    /** An attack move: its animation, the ticks until it reaches its target, how far it reaches. */
    record Attack(AnimationAccessor<? extends StaticAnimation> animation, int contact, double reach) {}

    /** The next move of the combo of the weapon the NPC holds (one-handed, two-handed, or bare hands). */
    static Attack attackFor(Mob npc, LivingEntityPatch<?> patch) {
        ItemStack stack = npc.getMainHandItem();
        CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(stack);
        WeaponCategory category = cap == null ? WeaponCategories.FIST : cap.getWeaponCategory();
        boolean twoHand = cap != null && cap.getStyle(patch) == CapabilityItem.Styles.TWO_HAND;
        List<AnimationAccessor<? extends StaticAnimation>> moves;
        int contact = 7;
        double reach = 3.2;
        if (category == WeaponCategories.GREATSWORD) {
            moves = List.of(Animations.GREATSWORD_AUTO1, Animations.GREATSWORD_AUTO2, Animations.BIPED_MOB_GREATSWORD);
            contact = 11;
            reach = 4.0;
        } else if (category == WeaponCategories.LONGSWORD) {
            moves = List.of(Animations.BIPED_MOB_LONGSWORD1, Animations.BIPED_MOB_LONGSWORD2);
            contact = 9;
            reach = 3.6;
        } else if (category == WeaponCategories.TACHI) {
            moves = List.of(Animations.BIPED_MOB_LONGSWORD1, Animations.BIPED_MOB_LONGSWORD2, Animations.BIPED_MOB_TACHI);
            contact = 9;
            reach = 3.6;
        } else if (category == WeaponCategories.UCHIGATANA) {
            moves = List.of(Animations.BIPED_MOB_UCHIGATANA1, Animations.BIPED_MOB_UCHIGATANA2, Animations.BIPED_MOB_UCHIGATANA3);
            contact = 8;
            reach = 3.6;
        } else if (category == WeaponCategories.SPEAR) {
            moves = twoHand ? List.of(Animations.BIPED_MOB_SPEAR_TWOHAND1, Animations.BIPED_MOB_SPEAR_TWOHAND2, Animations.BIPED_MOB_SPEAR_TWOHAND3)
                    : List.of(Animations.BIPED_MOB_SPEAR_ONEHAND);
            contact = 9;
            reach = 4.2;
        } else if (category == WeaponCategories.DAGGER) {
            moves = twoHand ? List.of(Animations.BIPED_MOB_DAGGER_TWOHAND1, Animations.BIPED_MOB_DAGGER_TWOHAND2)
                    : List.of(Animations.BIPED_MOB_DAGGER_ONEHAND1, Animations.BIPED_MOB_DAGGER_ONEHAND2, Animations.BIPED_MOB_DAGGER_ONEHAND3);
            contact = 5;
            reach = 2.8;
        } else if (category == WeaponCategories.SWORD && twoHand) {
            moves = List.of(Animations.BIPED_MOB_SWORD_DUAL1, Animations.BIPED_MOB_SWORD_DUAL2, Animations.BIPED_MOB_SWORD_DUAL3);
        } else if (category == WeaponCategories.TRIDENT) {
            moves = List.of(Animations.TRIDENT_AUTO1, Animations.TRIDENT_AUTO2, Animations.TRIDENT_AUTO3);
            contact = 8;
            reach = 4.0;
        } else if (category == WeaponCategories.FIST || category == WeaponCategories.NOT_WEAPON || stack.isEmpty()) {
            moves = List.of(Animations.FIST_AUTO1, Animations.FIST_AUTO2, Animations.FIST_AUTO3);
            contact = 5;
            reach = 2.8;
        } else {
            // Swords, axes and tools held in one hand
            moves = List.of(Animations.BIPED_MOB_ONEHAND1, Animations.BIPED_MOB_ONEHAND2);
        }
        int n = COMBO.merge(npc, 1, Integer::sum) - 1;
        return new Attack(moves.get(Math.floorMod(n, moves.size())), contact, reach);
    }
}
