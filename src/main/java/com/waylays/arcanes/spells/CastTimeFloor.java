package com.waylays.arcanes.spells;

import com.waylays.arcanes.ArcanesConfig;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Every spell a player casts takes at least a second to cast (server config {@code spells.minCastTicks}, 20 ticks):
 * Iron's own, this mod's and every add-on's. An instant spell is charged that long (Iron's waits for the cast time even
 * on instant spells: its cast simply comes at the end), a charged spell shorter than that is drawn out to it.
 * Channelled spells, recasts that a spell makes instant itself, and spells cast by mobs are left as they are.
 */
public final class CastTimeFloor {
    private CastTimeFloor() {}

    /** {@code ticks}, the spell's cast time for {@code caster}, raised to the minimum if it is a player's. */
    public static int apply(AbstractSpell spell, @Nullable LivingEntity caster, int ticks) {
        if (!(caster instanceof Player)) {
            return ticks;
        }
        int min = ArcanesConfig.minCastTicks();
        if (min <= 0 || ticks >= min) {
            return ticks;
        }
        CastType type = spell.getCastType();
        return type == CastType.INSTANT || type == CastType.LONG ? min : ticks;
    }

    /** Whether an instant spell is charged before it goes off (then its gesture comes at the end of the charge). */
    public static boolean chargesInstant(AbstractSpell spell) {
        return spell.getCastType() == CastType.INSTANT && ArcanesConfig.minCastTicks() > 0;
    }
}
