package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSchools;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Silenced (Seal of Silence): the bearer cannot cast spells. A player's casts are refused before they start (spellbook
 * and scrolls alike) and a cast already under way is broken off; Iron's spellcasting mobs lose their casts too.
 */
@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID)
public class SilenceEffect extends MobEffect {
    public SilenceEffect() {
        super(MobEffectCategory.HARMFUL, ModSchools.BOOK_COLOR);
    }

    @SubscribeEvent
    public static void onPreCast(SpellPreCastEvent event) {
        if (event.getEntity().hasEffect(ModEffects.SILENCE.get())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.arcanes_oublies.silenced").withStyle(ChatFormatting.DARK_PURPLE)));
            }
        }
    }

    /** A cast started before the seal, or by a mob, is broken off on the bearer's next tick. */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !entity.hasEffect(ModEffects.SILENCE.get())) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            if (MagicData.getPlayerMagicData(player).isCasting()) {
                Utils.serverSideCancelCast(player);
            }
        } else if (entity instanceof AbstractSpellCastingMob mob && mob.isCasting()) {
            mob.cancelCast();
        }
    }
}
