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
    /** A swing on its way: whom it is for, its move, the tick by which it must have hit, how far it may still reach. */
    private record Swing(LivingEntity target, AnimationAccessor<? extends StaticAnimation> move, int dueAt, double reach) {}

    private static final Map<Mob, Swing> SWINGS = new WeakHashMap<>();
    /** Set while the animation's hit is dealt again with Epic Fight's damage, so that it is let through. */
    private static final ThreadLocal<Boolean> STRIKING = ThreadLocal.withInitial(() -> false);
    /** How many animation hits landed, and how many of them were dealt when the swing itself passed beside (read by the CI check). */
    public static int animationHits, sureHits;
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
                        Swing pending = SWINGS.get(npc);
                        if (pending != null && pending.target() == target) {
                            SWINGS.remove(npc);
                        }
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
            Move move = attackFor(npc, patch);
            patch.playAnimationSynchronized(move.animation(), 0.0F);
            LAST_SWING.put(npc, npc.tickCount);
            STRUCK.put(npc, target);
            // Custom NPCs judged the target within reach: the swing will hit it, even if the animation's blade passes
            // beside (a target a little further than the move reaches, or one Epic Fight would spare)
            SWINGS.put(npc, new Swing(target, move.animation(), npc.tickCount + move.contact() + 4, Math.max(3.5, npc.distanceTo(target) + 1.5)));
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
            Mob npc = (Mob) e;
            dropEpicFightGoals(npc);
            Swing swing = SWINGS.get(npc);
            if (swing != null && npc.tickCount >= swing.dueAt()) {
                SWINGS.remove(npc);
                sureHit(npc, swing);
            }
        }
    }

    /** The swing passed beside its target: it is struck all the same, with the move's Epic Fight hit. */
    private static void sureHit(Mob npc, Swing swing) {
        LivingEntity target = swing.target();
        LivingEntityPatch<?> patch = patch(npc);
        if (patch == null || !npc.isAlive() || !target.isAlive() || target.level() != npc.level() || npc.distanceTo(target) > swing.reach()
                || !EpicNpc.fightsWithEpicFight(npc)) {
            return;
        }
        STRIKING.set(true);
        try {
            // Custom NPCs' melee strength (the base of the NPC's attack damage)
            float strength = (float) npc.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            if (target.hurt(patch.getDamageSource(swing.move(), net.minecraft.world.InteractionHand.MAIN_HAND), Math.max(.5F, strength))) {
                animationHits++;
                sureHits++;
            }
        } catch (RuntimeException ex) {
            if (!warned) {
                warned = true;
                ArcanesOublies.LOGGER.warn("An NPC's Epic Fight hit could not be dealt: {}", ex.toString());
            }
        } finally {
            STRIKING.set(false);
        }
    }

    /** Whom the NPC is fighting: whom Custom NPCs made it strike, its target, its attacker, or a mob that targets it. */
    static boolean fights(Mob npc, LivingEntity target) {
        return target == STRUCK.get(npc) || target == npc.getTarget() || target == npc.getLastHurtByMob()
                || target instanceof Mob m && m.getTarget() == npc;
    }

    /**
     * Already swinging: the strike Custom NPCs asks for now is dropped. Reeling from a blow does not stop it (an NPC
     * struck again and again would never strike back).
     */
    static boolean busy(Mob npc, LivingEntityPatch<?> patch) {
        Integer last = LAST_SWING.get(npc);
        if (last != null && npc.tickCount - last < 6) {
            return true;
        }
        try {
            return patch.getEntityState().attacking();
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
    /** An attack move: its animation, and the ticks until its blade reaches the target. */
    record Move(AnimationAccessor<? extends StaticAnimation> animation, int contact) {}

    static Move attackFor(Mob npc, LivingEntityPatch<?> patch) {
        ItemStack stack = npc.getMainHandItem();
        CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(stack);
        WeaponCategory category = cap == null ? WeaponCategories.FIST : cap.getWeaponCategory();
        boolean twoHand = cap != null && cap.getStyle(patch) == CapabilityItem.Styles.TWO_HAND;
        List<AnimationAccessor<? extends StaticAnimation>> moves;
        int contact = 8;
        if (category == WeaponCategories.GREATSWORD) {
            contact = 12;
            moves = List.of(Animations.GREATSWORD_AUTO1, Animations.GREATSWORD_AUTO2, Animations.BIPED_MOB_GREATSWORD);
        } else if (category == WeaponCategories.LONGSWORD) {
            contact = 10;
            moves = List.of(Animations.BIPED_MOB_LONGSWORD1, Animations.BIPED_MOB_LONGSWORD2);
        } else if (category == WeaponCategories.TACHI) {
            contact = 10;
            moves = List.of(Animations.BIPED_MOB_LONGSWORD1, Animations.BIPED_MOB_LONGSWORD2, Animations.BIPED_MOB_TACHI);
        } else if (category == WeaponCategories.UCHIGATANA) {
            contact = 9;
            moves = List.of(Animations.BIPED_MOB_UCHIGATANA1, Animations.BIPED_MOB_UCHIGATANA2, Animations.BIPED_MOB_UCHIGATANA3);
        } else if (category == WeaponCategories.SPEAR) {
            contact = 10;
            moves = twoHand ? List.of(Animations.BIPED_MOB_SPEAR_TWOHAND1, Animations.BIPED_MOB_SPEAR_TWOHAND2, Animations.BIPED_MOB_SPEAR_TWOHAND3)
                    : List.of(Animations.BIPED_MOB_SPEAR_ONEHAND);
        } else if (category == WeaponCategories.DAGGER) {
            contact = 6;
            moves = twoHand ? List.of(Animations.BIPED_MOB_DAGGER_TWOHAND1, Animations.BIPED_MOB_DAGGER_TWOHAND2)
                    : List.of(Animations.BIPED_MOB_DAGGER_ONEHAND1, Animations.BIPED_MOB_DAGGER_ONEHAND2, Animations.BIPED_MOB_DAGGER_ONEHAND3);
        } else if (category == WeaponCategories.SWORD && twoHand) {
            moves = List.of(Animations.BIPED_MOB_SWORD_DUAL1, Animations.BIPED_MOB_SWORD_DUAL2, Animations.BIPED_MOB_SWORD_DUAL3);
        } else if (category == WeaponCategories.TRIDENT) {
            contact = 9;
            moves = List.of(Animations.TRIDENT_AUTO1, Animations.TRIDENT_AUTO2, Animations.TRIDENT_AUTO3);
        } else if (category == WeaponCategories.FIST || category == WeaponCategories.NOT_WEAPON || stack.isEmpty()) {
            contact = 6;
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
        return new Move(moves.get(Math.floorMod(n, moves.size())), contact);
    }
}
