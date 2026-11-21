package com.waylays.arcanes.spells;

import com.waylays.arcanes.ArcanesConfig;
import com.waylays.arcanes.editor.SpellTuning;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Every spell a player casts takes at least a second to cast (server config {@code spells.minCastTicks}, 20 ticks):
 * Iron's own, this mod's and every add-on's. An instant spell is charged that long (Iron's waits for the cast time even
 * on instant spells: its cast simply comes at the end), a charged spell shorter than that is drawn out to it.
 * Channelled spells, recasts that a spell makes instant itself, and spells cast by mobs are left as they are. A cast
 * time set for a spell in the spell editor wins over all this (see {@link #forCast}).
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

    /**
     * The cast time of a cast a player starts: the one set in the spell editor if there is one (on an instant spell
     * too: it is then charged that long; 0 keeps it instant), else the spell's own raised to the minimum.
     */
    public static int forCast(AbstractSpell spell, @Nullable LivingEntity caster, int ticks) {
        if (caster instanceof Player) {
            Double tuned = SpellTuning.get(spell, SpellTuning.Stat.CAST_TIME);
            if (tuned != null && spell.getCastType() == CastType.INSTANT) {
                return Math.max(0, (int) Math.round(tuned));
            }
            if (tuned != null && spell.getCastType() == CastType.LONG) {
                // The editor wrote it into the spell: what Iron's computed from it is kept as it is
                return ticks;
            }
        }
        return apply(spell, caster, ticks);
    }

    /** How long an instant spell is charged before it goes off (0: not at all). */
    public static int instantCharge(AbstractSpell spell) {
        Double tuned = SpellTuning.get(spell, SpellTuning.Stat.CAST_TIME);
        return tuned != null ? Math.max(0, (int) Math.round(tuned)) : ArcanesConfig.minCastTicks();
    }

    /** Whether an instant spell is charged before it goes off (then its gesture comes at the end of the charge). */
    public static boolean chargesInstant(AbstractSpell spell) {
        return spell.getCastType() == CastType.INSTANT && instantCharge(spell) > 0;
    }
}
