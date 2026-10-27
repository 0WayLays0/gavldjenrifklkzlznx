package com.waylays.arcanes.spells.bone;

import com.waylays.arcanes.item.BoneBladeItem;
import com.waylays.arcanes.registry.ModItems;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Elbow Blade: a blade of bone splits out of the caster's elbow and into their hand: a real weapon (the Bone Blade,
 * see BoneBladeItem) for a while, put straight in an empty hand, else in the inventory; it crumbles when its time is up.
 * A caster with no inventory (a mob) gets the strength of it instead.
 */
@AutoSpellConfig
public class ElbowBladeSpell extends BoneSpell {
    public ElbowBladeSpell() {
        super("elbow_blade", SpellRarity.RARE, 6, 40, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    public static int duration(int spellLevel) {
        return 400 + 100 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.blade_damage", Utils.stringTruncation(BoneBladeItem.damage(spellLevel) + 1, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (entity instanceof Player player) {
            // A new blade replaces the old one
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i).is(ModItems.BONE_BLADE.get())) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
            ItemStack blade = BoneBladeItem.forge(level, spellLevel, duration(spellLevel));
            if (player.getMainHandItem().isEmpty()) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, blade);
            } else if (!inventory.add(blade)) {
                player.displayClientMessage(Component.translatable("ui.arcanes_oublies.blade_no_room"), true);
            }
        } else {
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration(spellLevel), 1));
        }
        BoneFx.splinters(level, entity.position().add(0, entity.getBbHeight() * .6, 0), 16, .3);
        BoneFx.marrow(level, entity.position().add(0, entity.getBbHeight() * .6, 0), 10, .3);
        BoneFx.crack(level, entity.position(), 1.2f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
