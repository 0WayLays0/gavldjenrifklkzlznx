package com.waylays.arcanes.entity.wind;

import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The sight of an Updraft: a column of wind spiralling up from the ground (the lift is dealt when cast, see UpdraftSpell). */
public class UpdraftGust extends WindVisual {
    public static final int LIFE = 30;

    public UpdraftGust(EntityType<? extends UpdraftGust> entityType, Level level) {
        super(entityType, level);
    }

    public static UpdraftGust at(Level level, Vec3 foot, float height) {
        UpdraftGust gust = new UpdraftGust(ModEntities.UPDRAFT.get(), level);
        gust.moveTo(foot.x, foot.y, foot.z, 0, 0);
        gust.setup(new Vector3f(0, 1, 0), height);
        level.addFreshEntity(gust);
        return gust;
    }

    @Override
    public int life() {
        return LIFE;
    }
}
