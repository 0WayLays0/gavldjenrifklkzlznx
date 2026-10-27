package com.waylays.arcanes.item;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.spells.bone.BoneFx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Bone Blade: a blade of bone grown out of the caster's elbow (Elbow Blade). It cuts harder the higher the spell's
 * level, reaches a little further than a sword, and leaves its victims bleeding. It is only lent: it crumbles away
 * when its time is up, wherever it is, and cannot be thrown down (a dropped one crumbles at once).
 */
public class BoneBladeItem extends SwordItem {
    private static final String EXPIRES = "ExpiresAt";
    private static final String LEVEL = "SpellLevel";

    public BoneBladeItem() {
        super(Tiers.IRON, new Properties().stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.UNBREAKABLE, new Unbreakable(false))
                .attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.2f)));
    }

    /** A blade for a spell of {@code spellLevel}, gone {@code duration} ticks from now. */
    public static ItemStack forge(Level level, int spellLevel, int duration) {
        ItemStack stack = new ItemStack(ModItems.BONE_BLADE.get());
        CompoundTag tag = new CompoundTag();
        tag.putLong(EXPIRES, level.getGameTime() + duration);
        tag.putInt(LEVEL, spellLevel);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage(spellLevel), AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.2, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ArcanesOublies.id("bone_blade_reach"), .75, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build());
        return stack;
    }

    /** The damage it adds to a bare hand's (iron sword: 5). */
    public static double damage(int spellLevel) {
        return 4.5 + .8 * spellLevel;
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static long expiresAt(ItemStack stack) {
        return data(stack).getLong(EXPIRES);
    }

    public static int spellLevel(ItemStack stack) {
        return Math.max(1, data(stack).getInt(LEVEL));
    }

    /** It crumbles: the stack is emptied, with splinters and a crack of bone. */
    private static void crumble(ItemStack stack, Level level, Entity holder) {
        stack.setCount(0);
        if (level instanceof ServerLevel server) {
            BoneFx.splinters(server, holder.position().add(0, holder.getBbHeight() * .6, 0), 14, .3);
            BoneFx.sound(server, holder.position(), SoundEvents.BONE_BLOCK_BREAK, 1, 1.2f);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && level.getGameTime() >= expiresAt(stack)) {
            crumble(stack, level, entity);
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 60, spellLevel(stack) / 3), attacker);
        if (target.level() instanceof ServerLevel server) {
            BoneFx.marrow(server, target.getBoundingBox().getCenter(), 6, .3);
        }
        return true;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        // Not to be thrown down: it stays in hand
        return false;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        // Dropped all the same (its holder died): it crumbles at once
        if (!entity.level().isClientSide) {
            BoneFx.splinters(entity.level(), entity.position(), 10, .2);
            entity.discard();
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item." + ArcanesOublies.MODID + ".bone_blade.bleeds").withStyle(ChatFormatting.DARK_RED));
        Level level = context.level();
        if (level != null) {
            long left = Math.max(0, expiresAt(stack) - level.getGameTime());
            tooltip.add(Component.translatable("item." + ArcanesOublies.MODID + ".bone_blade.crumbles", left / 20).withStyle(ChatFormatting.GRAY));
        }
    }
}
