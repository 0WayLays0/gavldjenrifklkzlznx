package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gold.GoldFx;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gilded Armor: a coat of liquid gold. Armor, toughness and knockback resistance, and part of every hit taken is
 * sent back to the attacker as Gold magic.
 */
@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID)
public class GildedArmorEffect extends MobEffect {
    public GildedArmorEffect() {
        super(MobEffectCategory.BENEFICIAL, ModSchools.GOLD_COLOR);
        // Values are per effect level (amplifier + 1)
        addAttributeModifier(Attributes.ARMOR, "c3f1a2b4-5d6e-4f70-8a91-b2c3d4e5f607", 3.0, AttributeModifier.Operation.ADDITION);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS, "d4a2b3c5-6e7f-4081-9ba2-c3d4e5f60718", 1.0, AttributeModifier.Operation.ADDITION);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, "e5b3c4d6-7f80-4192-acb3-d4e5f6071829", .1, AttributeModifier.Operation.ADDITION);
    }

    /** Share of the damage sent back to the attacker. */
    public static float reflectRatio(int amplifier) {
        return Math.min(.6f, .2f + .05f * amplifier);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 3 == 0;
    }

    /** A shimmering shell of gold dust turning around the body. */
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        float w = entity.getBbWidth() * .65f;
        float h = entity.getBbHeight();
        for (int i = 0; i < 3; i++) {
            float angle = entity.tickCount * .35f + i * Mth.TWO_PI / 3;
            float y = (float) ((entity.tickCount * .07 + i * .33) % 1.0) * h;
            SpellUtil.particles(entity.level(), i == 0 ? GoldFx.GOLD_DUST : GoldFx.PALE_DUST,
                    entity.getX() + Mth.cos(angle) * w, entity.getY() + y, entity.getZ() + Mth.sin(angle) * w, 1, 0, 0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void reflect(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        MobEffectInstance armor = victim.getEffect(ModEffects.GILDED_ARMOR.get());
        if (armor == null || victim.level().isClientSide || !(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim) {
            return;
        }
        // Never reflect a reflection (two gilded fighters would bounce it forever)
        if (event.getSource() instanceof SpellDamageSource spellSource && spellSource.spell() == ModSpells.GILDED_ARMOR.get()) {
            return;
        }
        float reflected = event.getAmount() * reflectRatio(armor.getAmplifier());
        if (reflected <= 0) {
            return;
        }
        DamageSources.applyDamage(attacker, reflected, SpellUtil.damageSource(ModSpells.GILDED_ARMOR.get(), victim, victim));
        double y = victim.getY() + victim.getBbHeight() * .6;
        SpellUtil.particles(victim.level(), GoldFx.sparks(), victim.getX(), y, victim.getZ(), 10, .3, .3, .3, .35);
        SpellUtil.particles(victim.level(), GoldFx.nuggetShards(), victim.getX(), y, victim.getZ(), 4, .2, .2, .2, .15);
        victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 1.2f, 1.6f);
        victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.5f, 1.2f);
    }
}
