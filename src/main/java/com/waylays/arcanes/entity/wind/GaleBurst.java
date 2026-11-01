package com.waylays.arcanes.entity.wind;

import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The sight of a Gale: a cone of wind blasting out from the hand (its push is dealt when it is cast, see GaleSpell). */
public class GaleBurst extends WindVisual {
    public static final int LIFE = 14;

    public GaleBurst(EntityType<? extends GaleBurst> entityType, Level level) {
        super(entityType, level);
    }

    public static GaleBurst at(Level level, Vec3 from, Vec3 dir, float range) {
        GaleBurst burst = new GaleBurst(ModEntities.GALE_BURST.get(), level);
        burst.moveTo(from.x, from.y, from.z, 0, 0);
        burst.setup(dir.normalize().toVector3f(), range);
        level.addFreshEntity(burst);
        return burst;
    }

    @Override
    public int life() {
        return LIFE;
    }
}
