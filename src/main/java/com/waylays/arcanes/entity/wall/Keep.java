package com.waylays.arcanes.entity.wall;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wall.WallFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Closing Keep: a ring of crenellated walls builds itself all round the target, then every wall slides inwards
 * at once, faster and faster, herding whatever is inside to the middle, where they meet: what is there is crushed,
 * thrown up and dazed, and the walls burst into flying bricks. The walls are {@link WallPiece}s (HERD); this keeps the
 * time and does the crushing. It draws nothing itself.
 */
public class Keep extends SpellEffectEntity {
    public static final int WALLS = 8;
    /** How far from the middle the walls stop and burst. */
    public static final float INNER = 2.2f;
    public static final int CLOSE_TICKS = 18;
    public static final float WALL_HEIGHT = 4;
    public static final float CRUSH_RADIUS = INNER + 1.6f;

    @Nullable
    private String spellId;
    private float radius = 9;
    private int closes = 60;

    public Keep(EntityType<? extends Keep> entityType, Level level) {
        super(entityType, level);
    }

    /** The keep round {@code at}, {@code radius} out; returns it (its walls are already rising). */
    public static Keep raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float radius, float damage) {
        Keep keep = new Keep(ModEntities.KEEP.get(), level);
        keep.setOwner(caster);
        keep.spellId = spell.getSpellId();
        keep.damage = damage;
        keep.radius = radius;
        keep.moveTo(at.x, at.y, at.z, 0, 0);
        float width = Mth.TWO_PI * radius / WALLS * 1.04f;
        WallPiece first = null;
        for (int k = 0; k < WALLS; k++) {
            float a = k * Mth.TWO_PI / WALLS;
            Vec3 p = SpellUtil.groundAt(level, at.add(Mth.cos(a) * radius, 0, Mth.sin(a) * radius), 4, 1);
            // Facing the middle
            float yaw = (float) Math.toDegrees(Math.atan2(Mth.cos(a), -Mth.sin(a)));
            WallPiece wall = WallPiece.raise(level, caster, spell, WallPiece.Kind.HERD, p, yaw, width, WALL_HEIGHT, radius - INNER, CLOSE_TICKS, 0);
            if (first == null) {
                first = wall;
            }
        }
        keep.closes = first.built() + CLOSE_TICKS;
        level.addFreshEntity(keep);
        WallFx.sigil(level, at, radius + 1, keep.closes + 8);
        WallFx.sound(level, at, SoundEvents.BEACON_ACTIVATE, 1.5f, .5f);
        return keep;
    }

    /** The tick the walls meet. */
    public int closes() {
        return closes;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > closes + 30) {
            discard();
            return;
        }
        if (tickCount < closes && tickCount % 3 == 0) {
            // Embers running round the ring as it tightens, the ground rumbling
            float progress = Mth.clamp((tickCount - (closes - CLOSE_TICKS)) / (float) CLOSE_TICKS, 0, 1);
            float r = radius - (radius - INNER) * progress * progress;
            for (int k = 0; k < 10; k++) {
                float a = (k + tickCount * .1f) * Mth.TWO_PI / 10;
                WallFx.embers(level, position().add(Mth.cos(a) * r, .2, Mth.sin(a) * r), 2, .2);
            }
            WallFx.sound(level, position(), SoundEvents.GRINDSTONE_USE, .9f, .3f + .3f * progress);
        }
        if (tickCount == closes) {
            crush(level);
        }
    }

    private void crush(ServerLevel level) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        Vec3 c = position();
        if (spell != null) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(CRUSH_RADIUS + 1, 6, CRUSH_RADIUS + 1))) {
                if (!e.isAlive() || SpellUtil.isAlly(caster, e)) {
                    continue;
                }
                double d = Math.hypot(e.getX() - c.x, e.getZ() - c.z);
                if (d > CRUSH_RADIUS + e.getBbWidth() * .5 || e.getY() > c.y + WALL_HEIGHT + 1 || e.getY() < c.y - 2) {
                    continue;
                }
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                e.setDeltaMovement(e.getDeltaMovement().multiply(.2, 0, .2).add(0, .9 * Math.max(.4, resist), 0));
                e.hurtMarked = true;
            }
        }
        WallFx.impact(level, c, CRUSH_RADIUS + 1, 16);
        WallFx.dust(level, c.add(0, 1.5, 0), 60, 2);
        WallFx.embers(level, c.add(0, 1.5, 0), 40, 1.6);
        WallFx.slam(level, c, 3f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.4f, .55f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.6f, .4f);
        SpellUtil.cameraShake(level, c, 24, radius + 16);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return false;
    }
}
