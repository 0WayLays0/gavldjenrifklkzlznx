package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.rune.RuneFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Edge Rune: a rune carved into the weapon. Every blow struck in close combat (a weapon, a fist, a fighting move)
 * deals the rune's damage on top and flashes its glyphs.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class RuneEdgeEffect extends MobEffect {
    public RuneEdgeEffect() {
        super(MobEffectCategory.BENEFICIAL, RuneFx.GLOW);
    }

    /** The extra damage of a blow struck by {@code attacker} under the rune of level {@code amplifier + 1}. */
    public static float bonus(LivingEntity attacker, int amplifier) {
        return ModSpells.EDGE_RUNE.get().getSpellPower(amplifier + 1, attacker);
    }

    private static boolean closeCombat(DamageSource source) {
        return source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)
                || source.is(ModSchools.PHYSICAL_STRIKE);
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker || !closeCombat(source)
                || source.is(DamageTypeTags.IS_PROJECTILE) || event.getNewDamage() <= 0) {
            return;
        }
        MobEffectInstance rune = attacker.getEffect(ModEffects.RUNE_EDGE);
        if (rune == null) {
            return;
        }
        LivingEntity target = event.getEntity();
        event.setNewDamage(event.getNewDamage() + bonus(attacker, rune.getAmplifier()));
        Vec3 at = target.position().add(0, target.getBbHeight() * .6, 0);
        RuneFx.glyphs(target.level(), at, 8, .25);
        SpellUtil.particles(target.level(), RuneFx.dust(RuneFx.SILVER, .8f), at.x, at.y, at.z, 6, .2, .2, .2, 0);
        RuneFx.sound(target.level(), at, SoundEvents.AMETHYST_BLOCK_HIT, 1f, 1.4f);
    }
}
