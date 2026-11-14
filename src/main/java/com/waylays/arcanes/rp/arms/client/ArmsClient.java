package com.waylays.arcanes.rp.arms.client;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.rp.arms.Arms;
import com.waylays.arcanes.rp.arms.Proc;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** The chance effects written under an item's name: "✦ 5 % on a blow → the target: Poison II (4 s)". */
@EventBusSubscriber(modid = ArcanesOublies.MODID, value = Dist.CLIENT)
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
        return Component.translatable(p.guard() ? "arms.arcanes_oublies.line_guard" : "arms.arcanes_oublies.line_strike", chance,
                        Component.translatable(p.self() ? "arms.arcanes_oublies.on_self" : "arms.arcanes_oublies.on_other"), what)
                .withColor(p.guard() ? 0xFF7FD4FF : 0xFFFFB45A);
    }

    static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        List<Proc> procs = event.getItemStack().get(Arms.PROCS.get());
        if (procs == null || procs.isEmpty()) {
            return;
        }
        event.getToolTip().add(Component.empty());
        for (Proc p : procs) {
            event.getToolTip().add(line(p));
        }
    }
}
