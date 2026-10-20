package com.waylays.arcanes.entity.boss;

import com.waylays.arcanes.ArcanesConfig;
import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.boss.BossAttack;
import com.waylays.arcanes.spells.boss.BossFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * The warning before a Boss Magic attack: a zone drawn on the ground (a circle, a ring, a line or a cone) under a
 * turning circle of runes, filling up until the attack lands. It is shown for {@link #warningTicks()} (3.5 s by
 * default, set in the server config) so whoever stands in it can see the danger and get out; only those still inside
 * when it lands are hit. Then the spell that drew it ({@link BossAttack}) strikes, and the zone flashes and fades.
 *
 * <p>Position: the centre of a circle, ring or cone, the start of a line. Yaw: the direction of a line or a cone.
 */
public class BossTelegraph extends SpellEffectEntity {
    /** Ticks the zone stays on after the attack, flashing and fading. */
    public static final int LINGER_TICKS = 12;
    /** How high above the drawn zone an attack still hits (and how far below). */
    public static final double DEFAULT_REACH_UP = 4;
    public static final double REACH_DOWN = 1.5;

    public enum Shape { CIRCLE, RING, LINE, CONE }

    /** The zone's colours: the danger zone itself, and its runes and edge. */
    public enum Style {
        CRIMSON(0xFF2438, 0xFFB040),
        HOLY(0xFFD25A, 0xFFF6D8),
        VOID(0x9A3CFF, 0xE0A8FF),
        EARTH(0xFF6A1A, 0xFFC27A),
        STAR(0x5AB8FF, 0xE8F6FF),
        /** Not a danger: a spot of shelter from a bigger attack. */
        SAFE(0x3CFF9A, 0xE8FFF2);

        public final int zone;
        public final int rune;

        Style(int zone, int rune) {
            this.zone = zone;
            this.rune = rune;
        }

        public static Style byId(int id) {
            Style[] all = values();
            return all[Mth.clamp(id, 0, all.length - 1)];
        }
    }

    private static final EntityDataAccessor<Integer> DATA_SHAPE = SynchedEntityData.defineId(BossTelegraph.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STYLE = SynchedEntityData.defineId(BossTelegraph.class, EntityDataSerializers.INT);
    /** Circle radius, ring outer radius, line length, cone radius. */
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(BossTelegraph.class, EntityDataSerializers.FLOAT);
    /** Ring inner radius, line width, cone half-angle in degrees. */
    private static final EntityDataAccessor<Float> DATA_SIZE2 = SynchedEntityData.defineId(BossTelegraph.class, EntityDataSerializers.FLOAT);
    /** Game tick the warning appeared and the tick the attack lands (game time, as an int). */
    private static final EntityDataAccessor<Integer> DATA_START = SynchedEntityData.defineId(BossTelegraph.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FIRE = SynchedEntityData.defineId(BossTelegraph.class, EntityDataSerializers.INT);
    /** Height of a great rune circle drawn in the sky above the zone (0: none). */
    private static final EntityDataAccessor<Float> DATA_SKY = SynchedEntityData.defineId(BossTelegraph.class, EntityDataSerializers.FLOAT);

    @Nullable
    private String spellId;
    private int spellLevel = 1;
    /** Which part of its spell this zone is (a spell may draw several, landing one after the other). */
    private String part = "";
    private double reachUp = DEFAULT_REACH_UP;
    private CompoundTag payload = new CompoundTag();
    private boolean fired;
    /** Client side: the renderer's record of the ground under the zone. */
    @Nullable
    public Object clientGround;

    public BossTelegraph(EntityType<? extends BossTelegraph> entityType, Level level) {
        super(entityType, level);
    }

    // ---------------------------------------------------------------- building one

    /** How long a warning stays before its attack lands (server config, 70 ticks by default). */
    public static int warningTicks() {
        return ArcanesConfig.bossWarningTicks();
    }

    private static BossTelegraph create(Level level, @Nullable LivingEntity caster, AbstractSpell spell, int spellLevel, Shape shape,
                                        Vec3 at, float yaw, float size, float size2) {
        BossTelegraph t = new BossTelegraph(ModEntities.BOSS_TELEGRAPH.get(), level);
        t.setOwner(caster);
        t.spellId = spell.getSpellId();
        t.spellLevel = spellLevel;
        t.moveTo(at.x, at.y, at.z, yaw, 0);
        t.entityData.set(DATA_SHAPE, shape.ordinal());
        t.entityData.set(DATA_SIZE, size);
        t.entityData.set(DATA_SIZE2, size2);
        int now = (int) level.getGameTime();
        t.entityData.set(DATA_START, now);
        t.entityData.set(DATA_FIRE, now + warningTicks());
        return t;
    }

    public static BossTelegraph circle(Level level, @Nullable LivingEntity caster, AbstractSpell spell, int spellLevel, Vec3 center, float radius) {
        return create(level, caster, spell, spellLevel, Shape.CIRCLE, center, 0, radius, 0);
    }

    public static BossTelegraph ring(Level level, @Nullable LivingEntity caster, AbstractSpell spell, int spellLevel, Vec3 center, float inner, float outer) {
        return create(level, caster, spell, spellLevel, Shape.RING, center, 0, outer, inner);
    }

    /** A strip {@code width} wide from {@code start}, {@code length} long toward {@code yaw}. */
    public static BossTelegraph line(Level level, @Nullable LivingEntity caster, AbstractSpell spell, int spellLevel, Vec3 start, float yaw, float length, float width) {
        return create(level, caster, spell, spellLevel, Shape.LINE, start, yaw, length, width);
    }

    public static BossTelegraph cone(Level level, @Nullable LivingEntity caster, AbstractSpell spell, int spellLevel, Vec3 apex, float yaw, float radius, float halfAngle) {
        return create(level, caster, spell, spellLevel, Shape.CONE, apex, yaw, radius, halfAngle);
    }

    /** Lands {@code ticks} later than a plain warning would (the warning still shows from now). */
    public BossTelegraph delayed(int ticks) {
        this.entityData.set(DATA_FIRE, getFireTick() + ticks);
        return this;
    }

    public BossTelegraph style(Style style) {
        this.entityData.set(DATA_STYLE, style.ordinal());
        return this;
    }

    public BossTelegraph sky(float height) {
        this.entityData.set(DATA_SKY, height);
        return this;
    }

    public BossTelegraph damage(float damage) {
        this.damage = damage;
        return this;
    }

    public BossTelegraph part(String part) {
        this.part = part;
        return this;
    }

    public BossTelegraph reachUp(double reachUp) {
        this.reachUp = reachUp;
        return this;
    }

    public BossTelegraph payload(CompoundTag payload) {
        this.payload = payload;
        return this;
    }

    /** Adds it to the world and sounds the warning. */
    public BossTelegraph spawn() {
        level().addFreshEntity(this);
        Vec3 c = center();
        float pitch = getStyle() == Style.SAFE ? 1.6f : .6f;
        level().playSound(null, c.x, c.y, c.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.4f, pitch);
        return this;
    }

    // ---------------------------------------------------------------- reading it

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SHAPE, 0);
        builder.define(DATA_STYLE, 0);
        builder.define(DATA_SIZE, 1f);
        builder.define(DATA_SIZE2, 0f);
        builder.define(DATA_START, 0);
        builder.define(DATA_FIRE, 0);
        builder.define(DATA_SKY, 0f);
    }

    public Shape getShape() {
        Shape[] all = Shape.values();
        return all[Mth.clamp(this.entityData.get(DATA_SHAPE), 0, all.length - 1)];
    }

    public Style getStyle() {
        return Style.byId(this.entityData.get(DATA_STYLE));
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public float getSize2() {
        return this.entityData.get(DATA_SIZE2);
    }

    public int getStartTick() {
        return this.entityData.get(DATA_START);
    }

    public int getFireTick() {
        return this.entityData.get(DATA_FIRE);
    }

    public float getSkyHeight() {
        return this.entityData.get(DATA_SKY);
    }

    public String getPart() {
        return part;
    }

    public int getSpellLevel() {
        return spellLevel;
    }

    public float getDamage() {
        return damage;
    }

    public CompoundTag getPayload() {
        return payload;
    }

    @Nullable
    public AbstractSpell getSpell() {
        return spellId == null ? null : SpellRegistry.getSpell(spellId);
    }

    /** 0 when the warning appears, 1 when the attack lands (partial ticks for smooth drawing). */
    public float progress(float partialTicks) {
        float now = (int) level().getGameTime() + partialTicks;
        int start = getStartTick();
        int fire = getFireTick();
        return fire <= start ? 1 : Mth.clamp((now - start) / (fire - start), 0, 1);
    }

    /** Ticks since the attack landed (negative before). */
    public float sinceFired(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - getFireTick();
    }

    /** Unit vector along the line or the cone. */
    public Vec3 forward() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    /** The middle of the zone (a line's middle, not its start). */
    public Vec3 center() {
        return getShape() == Shape.LINE ? position().add(forward().scale(getSize() * .5)) : position();
    }

    /** Whether a point is in the danger zone (between {@link #REACH_DOWN} below it and the reach above). */
    public boolean contains(Vec3 p) {
        double dy = p.y - getY();
        if (dy < -REACH_DOWN || dy > reachUp) {
            return false;
        }
        double dx = p.x - getX();
        double dz = p.z - getZ();
        double r2 = dx * dx + dz * dz;
        float size = getSize();
        return switch (getShape()) {
            case CIRCLE -> r2 <= size * size;
            case RING -> r2 <= size * size && r2 >= getSize2() * getSize2();
            case LINE -> {
                Vec3 f = forward();
                double along = dx * f.x + dz * f.z;
                double across = dx * f.z - dz * f.x;
                yield along >= 0 && along <= size && Math.abs(across) <= getSize2() * .5;
            }
            case CONE -> {
                if (r2 > size * size) {
                    yield false;
                }
                if (r2 < 1.0E-6) {
                    yield true;
                }
                Vec3 f = forward();
                double cos = (dx * f.x + dz * f.z) / Math.sqrt(r2);
                yield cos >= Math.cos(Math.toRadians(getSize2()));
            }
        };
    }

    /** The box around the whole zone. */
    public AABB zoneBox() {
        double reach = switch (getShape()) {
            case LINE -> getSize() + getSize2();
            default -> getSize();
        };
        return new AABB(getX() - reach, getY() - REACH_DOWN, getZ() - reach, getX() + reach, getY() + reachUp, getZ() + reach);
    }

    /** Living things standing in the zone right now that the caster may hurt. */
    public List<LivingEntity> targetsInside() {
        Entity caster = getOwner();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, zoneBox())) {
            if (e.isAlive() && !e.isSpectator() && contains(e.position()) && !SpellUtil.isAlly(caster, e)) {
                out.add(e);
            }
        }
        return out;
    }

    /** Hurts {@code target} with this zone's spell, credited to the caster. */
    public boolean strike(LivingEntity target, float amount) {
        AbstractSpell spell = getSpell();
        if (spell == null) {
            return false;
        }
        return DamageSources.applyDamage(target, amount, SpellUtil.damageSource(spell, this, getOwner()));
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int now = (int) level.getGameTime();
        int left = getFireTick() - now;
        if (!fired) {
            if (left == 20) {
                Vec3 c = center();
                level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.5f, getStyle() == Style.SAFE ? 1.8f : .7f);
            }
            // The last second: the edge pulses with particles, in case the drawing is hidden by grass or snow
            if (left <= 20 && left % 4 == 0) {
                BossFx.outline(level, this);
            }
            if (left <= 0) {
                fired = true;
                if (getSpell() instanceof BossAttack attack) {
                    attack.onTelegraphFire(this, level);
                }
            }
        } else if (left <= -LINGER_TICKS) {
            discard();
        }
        // A zone never outstays its welcome (a spell that forgot it, a stopped clock)
        if (tickCount > 1200) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (spellId != null) {
            tag.putString("Spell", spellId);
        }
        tag.putInt("SpellLevel", spellLevel);
        tag.putString("Part", part);
        tag.put("Payload", payload);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spellId = tag.contains("Spell") ? tag.getString("Spell") : null;
        spellLevel = Math.max(1, tag.getInt("SpellLevel"));
        part = tag.getString("Part");
        payload = tag.getCompound("Payload");
    }
}
