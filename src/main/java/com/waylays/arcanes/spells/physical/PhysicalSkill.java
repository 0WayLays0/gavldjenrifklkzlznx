package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModAttributes;
import com.waylays.arcanes.registry.ModSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A move of Physical Prowess. Not magic: it sits in a spellbook or on a weapon like a spell, but its strength comes
 * from its level and the fighter's Physical Power (never from spell power), and blades add their own damage: each
 * blow deals a share of the move's strength plus a share of what the fighter hits with (bare fist 1, a sword's
 * damage, Strength and Sharpness included). Moves play out over a few ticks, timed on the fighter's animation
 * (see {@link SkillTimeline}), and make their own sounds.
 */
public abstract class PhysicalSkill extends AbstractSpell {
    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;

    protected PhysicalSkill(String name, SpellRarity minRarity, int maxLevel, int cooldownSeconds) {
        this.spellId = ArcanesOublies.id(name);
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(minRarity)
                .setSchoolResource(ModSchools.PHYSICAL_RESOURCE)
                .setMaxLevel(maxLevel)
                .setCooldownSeconds(cooldownSeconds)
                .build();
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    /** The move's animation plays to its end. */
    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }

    /** The move's own strength: its level and the fighter's Physical Power (and the server's power setting). */
    public float getStrength(int spellLevel, @Nullable LivingEntity fighter) {
        double power = 1;
        if (fighter != null) {
            AttributeInstance attribute = fighter.getAttribute(ModAttributes.PHYSICAL_SPELL_POWER);
            if (attribute != null) {
                power = attribute.getValue();
            }
        }
        return (float) ((baseSpellPower + spellPowerPerLevel * (spellLevel - 1)) * ServerConfigs.getSpellConfig(this).powerMultiplier() * power);
    }

    /** What the fighter hits with: their attack damage (1 bare-handed, a weapon's damage, Strength), plus Sharpness and the like against {@code target}. */
    public static float weaponDamage(LivingEntity fighter, @Nullable LivingEntity target) {
        AttributeInstance attack = fighter.getAttribute(Attributes.ATTACK_DAMAGE);
        float damage = attack == null ? 1 : (float) attack.getValue();
        if (target != null && fighter.level() instanceof ServerLevel level) {
            DamageSource source = fighter instanceof Player player ? fighter.damageSources().playerAttack(player) : fighter.damageSources().mobAttack(fighter);
            damage = EnchantmentHelper.modifyDamage(level, fighter.getMainHandItem(), target, source, damage);
        }
        return damage;
    }

    /** A shot's bonus from an enchantment of the held weapon (a bow's Power): +{@code perLevel} per level. */
    public static float enchantBonus(LivingEntity fighter, ResourceKey<Enchantment> enchantment, float perLevel) {
        return 1 + perLevel * enchantLevel(fighter, enchantment);
    }

    /** The level of an enchantment on the held weapon (0 without it). */
    public static int enchantLevel(LivingEntity fighter, ResourceKey<Enchantment> enchantment) {
        return fighter.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(enchantment)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, fighter.getMainHandItem()))
                .orElse(0);
    }

    /** One blow: {@code strengthShare} of the move's strength plus {@code weaponShare} of the weapon's damage. */
    protected float blow(int spellLevel, LivingEntity fighter, LivingEntity target, float strengthShare, float weaponShare) {
        return getStrength(spellLevel, fighter) * strengthShare + weaponDamage(fighter, target) * weaponShare;
    }

    protected static MutableComponent damageLine(float damage) {
        return Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(damage, 1));
    }

    protected static MutableComponent weaponLine(float share) {
        return Component.translatable("ui.arcanes_oublies.weapon_share", Math.round(share * 100));
    }

    /** Tells a player why the move cannot be done (on the action bar). */
    protected static void refuse(LivingEntity fighter, String key) {
        if (fighter instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(key).withStyle(ChatFormatting.RED)));
        }
    }
}
