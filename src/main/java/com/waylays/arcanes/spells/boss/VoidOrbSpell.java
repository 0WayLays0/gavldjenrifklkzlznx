package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.entity.boss.VoidOrb;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Void Orb: a great sphere of darkness rolls slowly forward along a marked path (touching it hurts, standing near it
 * drags you toward it). Where the path ends, two circles wait for it: when it gets there it collapses, pulling in
 * everything around, then bursts.
 */
@AutoSpellConfig
public class VoidOrbSpell extends BossSpell {
    public static final float PATH_WIDTH = 3f;
    public static final float COLLAPSE_RADIUS = 6.5f;
    public static final float BURST_RADIUS = 4.5f;
    /** The orb arrives this long after its path's warning ran out; the burst follows the collapse after {@link #BURST_DELAY}. */
    public static final int EXTRA_TRAVEL = 10;
    public static final int BURST_DELAY = 18;
    public static final double HEIGHT = 1.4;

    public VoidOrbSpell() {
        super("void_orb", SpellRarity.EPIC, 3, 30, BossAnimations.BEAM);
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 90;
    }

    public static float range(int spellLevel) {
        return 18 + 2 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster) * 1.3f, 1)),
                Component.translatable("ui.arcanes_oublies.contact_damage", Utils.stringTruncation(getDamage(spellLevel, caster) * .4f, 1)),
                Component.translatable("ui.arcanes_oublies.reach", Utils.stringTruncation(range(spellLevel), 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENDERMAN_STARE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float yaw = facing(entity);
        Vec3 dir = direction(yaw);
        Vec3 ground = feet(entity).add(dir.scale(1.5));
        Vec3 from = ground.add(0, HEIGHT, 0);
        // The orb stops short of a wall
        HitResult wall = level.clip(new ClipContext(from, from.add(dir.scale(range(spellLevel))), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        Vec3 to = wall.getType() == HitResult.Type.MISS ? from.add(dir.scale(range(spellLevel))) : wall.getLocation().subtract(dir.scale(VoidOrb.RADIUS + .3));
        float distance = (float) Math.max(1, to.subtract(from).horizontalDistance());
        Vec3 end = SpellUtil.groundAt(level, to, 6, 1);
        float damage = getDamage(spellLevel, entity);
        BossTelegraph path = BossTelegraph.line(level, entity, this, spellLevel, ground, yaw, distance, PATH_WIDTH)
                .style(BossTelegraph.Style.VOID).delayed(EXTRA_TRAVEL).part("path").spawn();
        BossTelegraph.circle(level, entity, this, spellLevel, end, COLLAPSE_RADIUS)
                .style(BossTelegraph.Style.VOID).delayed(EXTRA_TRAVEL).damage(damage * .3f).part("collapse").spawn();
        BossTelegraph.circle(level, entity, this, spellLevel, end, BURST_RADIUS)
                .style(BossTelegraph.Style.CRIMSON).delayed(EXTRA_TRAVEL + BURST_DELAY).damage(damage * 1.3f).part("burst").spawn();
        int arrive = path.getFireTick();
        VoidOrb.spawn(level, entity, this, from, end.add(0, HEIGHT, 0), now(level), arrive, arrive + BURST_DELAY, damage * .4f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        switch (t.getPart()) {
            case "collapse" -> {
                // Everything around is dragged into the middle
                for (LivingEntity target : t.targetsInside()) {
                    t.strike(target, t.getDamage());
                    Vec3 in = c.subtract(target.position()).multiply(1, 0, 1);
                    target.setDeltaMovement(in.scale(.28).add(0, .25, 0));
                    target.hurtMarked = true;
                }
                SpellUtil.particles(level, ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1, c.z, 120, t.getSize() * .5, 1, t.getSize() * .5, .4);
                level.playSound(null, c.x, c.y, c.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 3f, .4f);
                level.playSound(null, c.x, c.y, c.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.HOSTILE, 3f, .6f);
            }
            case "burst" -> {
                for (LivingEntity target : t.targetsInside()) {
                    t.strike(target, t.getDamage());
                    knockAway(target, c, 1.3, .6);
                }
                SpellUtil.particles(level, ParticleTypes.DRAGON_BREATH, c.x, c.y + 1, c.z, 80, t.getSize() * .4, .8, t.getSize() * .4, .12);
                BossFx.impact(level, c, t.getSize(), 0x9A3CFF, SoundEvents.WITHER_BREAK_BLOCK);
            }
            default -> {
            }
        }
    }
}
