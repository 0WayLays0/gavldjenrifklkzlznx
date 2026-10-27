package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/**
 * Cosmos, the magic of space and time. Two palettes: time is the warm orange of the Time Variance Authority's
 * retro-futuristic doors and clocks; space is deep blue and violet sown with white and gold stars. And who the
 * school's spells count as friend or foe.
 */
public final class Cosmos {
    /** Time: the TVA's orange, its amber light, deep ember, cream-white, the bronze of its clocks. */
    public static final int ORANGE = 0xFF8A1E;
    public static final int AMBER = 0xFFB347;
    public static final int EMBER = 0xD9480F;
    public static final int CREAM = 0xFFE7C2;
    public static final int BRONZE = 0x5C3416;
    /** Space: violet and blue nebulae, white and gold stars, comet ice, the deep of the night. */
    public static final int VIOLET = 0x7A4CFF;
    public static final int BLUE = 0x3F6BFF;
    public static final int STAR = 0xF4F6FF;
    public static final int GOLD = 0xFFD77A;
    public static final int ICE = 0x9FE8FF;
    public static final int DEEP = 0x101238;

    private Cosmos() {}

    /** The caster, its summons, its team, and what Iron's considers friendly fire. */
    public static boolean isAlly(@Nullable Entity caster, Entity other) {
        return SpellUtil.isAlly(caster, other);
    }

    /**
     * A foe of the caster, not just anyone who is not its ally: a monster, something hunting the caster or its allies,
     * or something that fought the caster lately (players included). Animals and villagers minding their own business
     * are left alone by the spells that sort friend from foe.
     */
    public static boolean isFoe(@Nullable LivingEntity caster, LivingEntity other) {
        if (caster == null || !other.isAlive() || other instanceof ArmorStand || isAlly(caster, other)) {
            return false;
        }
        if (other instanceof Enemy) {
            return true;
        }
        if (other instanceof Mob mob && mob.getTarget() != null && (mob.getTarget() == caster || isAlly(caster, mob.getTarget()))) {
            return true;
        }
        if (caster.getLastHurtByMob() == other || caster.getLastHurtMob() == other) {
            return true;
        }
        return other instanceof Player player && player.getLastHurtMob() == caster;
    }

    /**
     * At or under this health after a blow, a creature is pruned from time: a share of its greatest health growing with
     * the spell's level (10% plus 2% a level, at most 30%), never under 3.
     */
    public static float pruneLine(int spellLevel, float maxHealth) {
        return Math.max(3, maxHealth * Math.min(.3f, .1f + .02f * spellLevel));
    }

    /** Bosses are never pruned outright: they only take the blow's damage. */
    public static boolean isBoss(Entity entity) {
        return entity.getType().is(Tags.EntityTypes.BOSSES);
    }
}
