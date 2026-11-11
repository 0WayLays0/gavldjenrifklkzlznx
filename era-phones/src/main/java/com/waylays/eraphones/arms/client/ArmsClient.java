package com.waylays.eraphones.arms.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.arms.Arms;
import com.waylays.eraphones.arms.Proc;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** The chance effects written under an item's name: "5 % on a blow: sets on fire (4 s)". */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class ArmsClient {
    private ArmsClient() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        List<Proc> procs = event.getItemStack().get(Arms.PROCS.get());
        if (procs == null || procs.isEmpty()) {
            return;
        }
        event.getToolTip().add(Component.empty());
        for (Proc p : procs) {
            Proc.Kind k = Proc.Kind.of(p.type());
            if (k == null) {
                continue;
            }
            String chance = p.chance() == Math.rint(p.chance()) ? String.valueOf((int) p.chance()) : String.valueOf(p.chance());
            Component what = Component.translatable("arms.eraphones.proc." + k.id);
            String details = (k.usesPower ? " " + p.power() : "") + (k.usesSeconds ? " (" + p.seconds() + " s)" : "");
            event.getToolTip().add(Component.translatable(k.strike ? "arms.eraphones.line_strike" : "arms.eraphones.line_guard", chance, what, details)
                    .withStyle(k.strike ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        }
    }
}
