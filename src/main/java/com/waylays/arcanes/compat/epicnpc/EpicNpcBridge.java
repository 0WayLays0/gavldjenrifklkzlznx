package com.waylays.arcanes.compat.epicnpc;

import com.waylays.arcanes.ArcanesOublies;
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
import java.util.WeakHashMap;

/**
 * What makes Custom NPCs' NPCs fight like Epic Fight's (only loaded when both mods are there, see {@link EpicNpc}).
 * Custom NPCs keeps deciding who an NPC fights, where it goes and when it strikes, so everything set in its menus
 * still holds; Epic Fight shows the strike and deals it. When Custom NPCs makes the NPC strike, its own blow is
 * dropped and the attack animation of the weapon in its hand plays instead (one move after the other, as a combo).
 * The animation's own hit is then the NPC's only blow: Epic Fight's hit, with Custom NPCs' melee strength and effects,
 * and only on whom the NPC is fighting. Epic Fight's own attack and chase goals are taken off the NPCs. An NPC that
 * does not fight with Epic Fight ({@link EpicNpc#fightsWithEpicFight}) keeps Custom NPCs' own blow and swing.
 */
public final class EpicNpcBridge {
    private static final Map<Mob, Integer> COMBO = new WeakHashMap<>();
    private static final Map<Mob, Integer> LAST_SWING = new WeakHashMap<>();
    /** Whom Custom NPCs last made each NPC strike. */
    private static final Map<Mob, LivingEntity> STRUCK = new WeakHashMap<>();
    /** Set while the animation's hit is dealt again with Epic Fight's damage, so that it is let through. */
    private static final ThreadLocal<Boolean> STRIKING = ThreadLocal.withInitial(() -> false);
    /** How many animation hits landed (read by the CI check). */
    public static int animationHits;
    private static boolean warned;

    private EpicNpcBridge() {}

    public static boolean isNpc(Entity e) {
        return EpicNpc.isNpc(e);
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
        if (attacker == null || attacker.level().isClientSide() || STRIKING.get() || !isNpc(attacker)
                || source instanceof EpicFightDamageSource || source.getDirectEntity() != attacker
                // Switched to Custom NPCs' plain combat, or drawn by Custom NPCs (Epic Fight's swing would not show)
                || !EpicNpc.fightsWithEpicFight(attacker)) {
            return;
        }
        Mob npc = (Mob) attacker;
        LivingEntityPatch<?> patch = patch(npc);
        if (patch == null) {
            return;
        }
        LivingEntity target = event.getEntity();
        EpicFightDamageSource swing = patch.getEpicFightDamageSource();
        if (swing != null) {
            // The animation reached someone: Epic Fight asks Custom NPCs for the blow, which comes back as a plain
            // one. It is dealt again as Epic Fight's hit, on whom the NPC fights only.
            event.setCanceled(true);
            if (fights(npc, target)) {
                STRIKING.set(true);
                try {
                    if (target.hurt(swing, event.getAmount())) {
                        animationHits++;
                    }
                } finally {
                    STRIKING.set(false);
                }
            }
            return;
        }
        // Custom NPCs strikes: no blow of its own, the attack animation plays and its swing will hit
        if (busy(npc, patch)) {
            event.setCanceled(true);
            return;
        }
        try {
            patch.playAnimationSynchronized(attackFor(npc, patch), 0.0F);
            LAST_SWING.put(npc, npc.tickCount);
            STRUCK.put(npc, target);
            event.setCanceled(true);
        } catch (RuntimeException ex) {
            // An animation that could not be played: the blow lands as it would without Epic Fight
            if (!warned) {
                warned = true;
                ArcanesOublies.LOGGER.warn("An NPC's Epic Fight attack could not be played: {}", ex.toString());
            }
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        Entity e = event.getEntity();
        if (!e.level().isClientSide() && isNpc(e)) {
            dropEpicFightGoals((Mob) e);
        }
    }

    /** Whom the NPC is fighting: whom Custom NPCs made it strike, its target, its attacker, or a mob that targets it. */
    static boolean fights(Mob npc, LivingEntity target) {
        return target == STRUCK.get(npc) || target == npc.getTarget() || target == npc.getLastHurtByMob()
                || target instanceof Mob m && m.getTarget() == npc;
    }

    /** Already swinging (or reeling from a blow): the strike Custom NPCs asks for now is dropped. */
    static boolean busy(Mob npc, LivingEntityPatch<?> patch) {
        Integer last = LAST_SWING.get(npc);
        if (last != null && npc.tickCount - last < 6) {
            return true;
        }
        try {
            return patch.getEntityState().attacking() || patch.getEntityState().inaction();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** Epic Fight's own attack and chase goals (put back each time the NPC's weapon changes): Custom NPCs fights. */
    static void dropEpicFightGoals(Mob npc) {
        List<Goal> drop = null;
        for (WrappedGoal w : npc.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof AnimatedAttackGoal || w.getGoal() instanceof TargetChasingGoal) {
                if (drop == null) {
                    drop = new ArrayList<>();
                }
                drop.add(w.getGoal());
            }
        }
        if (drop != null) {
            drop.forEach(npc.goalSelector::removeGoal);
        }
    }

    /** The next move of the combo of the weapon the NPC holds (one-handed, two-handed, or bare hands). */
    static AnimationAccessor<? extends StaticAnimation> attackFor(Mob npc, LivingEntityPatch<?> patch) {
        ItemStack stack = npc.getMainHandItem();
        CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(stack);
        WeaponCategory category = cap == null ? WeaponCategories.FIST : cap.getWeaponCategory();
        boolean twoHand = cap != null && cap.getStyle(patch) == CapabilityItem.Styles.TWO_HAND;
        List<AnimationAccessor<? extends StaticAnimation>> moves;
        if (category == WeaponCategories.GREATSWORD) {
            moves = List.of(Animations.GREATSWORD_AUTO1, Animations.GREATSWORD_AUTO2, Animations.BIPED_MOB_GREATSWORD);
        } else if (category == WeaponCategories.LONGSWORD) {
            moves = List.of(Animations.BIPED_MOB_LONGSWORD1, Animations.BIPED_MOB_LONGSWORD2);
        } else if (category == WeaponCategories.TACHI) {
            moves = List.of(Animations.BIPED_MOB_LONGSWORD1, Animations.BIPED_MOB_LONGSWORD2, Animations.BIPED_MOB_TACHI);
        } else if (category == WeaponCategories.UCHIGATANA) {
            moves = List.of(Animations.BIPED_MOB_UCHIGATANA1, Animations.BIPED_MOB_UCHIGATANA2, Animations.BIPED_MOB_UCHIGATANA3);
        } else if (category == WeaponCategories.SPEAR) {
            moves = twoHand ? List.of(Animations.BIPED_MOB_SPEAR_TWOHAND1, Animations.BIPED_MOB_SPEAR_TWOHAND2, Animations.BIPED_MOB_SPEAR_TWOHAND3)
                    : List.of(Animations.BIPED_MOB_SPEAR_ONEHAND);
        } else if (category == WeaponCategories.DAGGER) {
            moves = twoHand ? List.of(Animations.BIPED_MOB_DAGGER_TWOHAND1, Animations.BIPED_MOB_DAGGER_TWOHAND2)
                    : List.of(Animations.BIPED_MOB_DAGGER_ONEHAND1, Animations.BIPED_MOB_DAGGER_ONEHAND2, Animations.BIPED_MOB_DAGGER_ONEHAND3);
        } else if (category == WeaponCategories.SWORD && twoHand) {
            moves = List.of(Animations.BIPED_MOB_SWORD_DUAL1, Animations.BIPED_MOB_SWORD_DUAL2, Animations.BIPED_MOB_SWORD_DUAL3);
        } else if (category == WeaponCategories.TRIDENT) {
            moves = List.of(Animations.TRIDENT_AUTO1, Animations.TRIDENT_AUTO2, Animations.TRIDENT_AUTO3);
        } else if (category == WeaponCategories.FIST || category == WeaponCategories.NOT_WEAPON || stack.isEmpty()) {
            moves = List.of(Animations.FIST_AUTO1, Animations.FIST_AUTO2, Animations.FIST_AUTO3);
        } else {
            // Swords, axes and tools held in one hand
            moves = List.of(Animations.BIPED_MOB_ONEHAND1, Animations.BIPED_MOB_ONEHAND2);
        }
        Integer last = LAST_SWING.get(npc);
        if (last == null || npc.tickCount - last > 60) {
            COMBO.remove(npc);
        }
        int n = COMBO.merge(npc, 1, Integer::sum) - 1;
        return moves.get(Math.floorMod(n, moves.size()));
    }
}
