package com.waylays.eraphones.arms.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.arms.Arms;
import com.waylays.eraphones.arms.Proc;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** The chance effects written under an item's name: "✦ 5 % on a blow → the target: Poison II (4 s)". */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class ArmsClient {
    private ArmsClient() {}

    public static Component line(Proc raw) {
        Proc p = raw.normalized();
        String chance = p.chance() == Math.rint(p.chance()) ? String.valueOf((int) p.chance()) : String.valueOf(p.chance());
        MutableComponent what = Effects.name(p.type()).copy();
        if (!p.special() && p.power() > 1) {
            what.append(" " + roman(p.power()));
        } else if (p.special() && p.power() > 0 && !p.type().equals("*feu")) {
            what.append(" " + p.power() + (p.type().equals("*vol_de_vie") ? " %" : ""));
        }
        if (p.seconds() > 0 && !p.type().equals("*foudre") && !p.type().equals("*degats") && !p.type().equals("*epines")
                && !p.type().equals("*soin") && !p.type().equals("*vol_de_vie") && !p.type().equals("*recul")) {
            what.append(" (" + p.seconds() + " s)");
        }
        return Component.translatable(p.guard() ? "arms.eraphones.line_guard" : "arms.eraphones.line_strike", chance,
                        Component.translatable(p.self() ? "arms.eraphones.on_self" : "arms.eraphones.on_other"), what)
                .withColor(p.guard() ? 0xFF7FD4FF : 0xFFFFB45A);
    }

    static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        List<Proc> procs = Arms.PROCS.get().get(event.getItemStack());
        if (procs == null || procs.isEmpty()) {
            return;
        }
        event.getToolTip().add(Component.empty());
        for (Proc p : procs) {
            event.getToolTip().add(line(p));
        }
    }
}
