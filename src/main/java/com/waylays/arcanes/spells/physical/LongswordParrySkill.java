package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.physical.StrikeTrail;
import com.waylays.arcanes.network.ArcanesNetwork;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Longsword 3, Parry and Riposte: the longsword raised in a guard for about a second. The first blow that comes from
 * in front while it is up is caught on the blade: a weapon blow (or a fighting move) does nothing, the attacker is
 * thrown off balance (slowed and weakened) and gets the point straight back; an arrow or a thrown weapon is turned
 * aside. Magic is not something a blade can parry.
 */
@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID)
@AutoSpellConfig
public class LongswordParrySkill extends PhysicalSkill {
    /** The riposte lands this long after the parry. */
    public static final int RIPOSTE_TICK = 3;
    public static final double RIPOSTE_REACH = 4;
    /** Blows from further round than this (degrees either side of where the fighter faces) are not caught. */
    public static final double GUARD_ANGLE = 70;

    private record Guard(int spellLevel, long until) {}

    private static final Map<UUID, Guard> GUARDS = new HashMap<>();

    public LongswordParrySkill() {
        super("longsword_parry", SpellRarity.RARE, 5, 10);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.arcanes_oublies.parry_window", Utils.timeFromTicks(getParryTicks(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.riposte", damageLine(getStrength(spellLevel, caster) * .8f)), weaponLine(1.3f));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return PhysicalAnimations.LONGSWORD_GUARD;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        GUARDS.put(entity.getUUID(), new Guard(spellLevel, level.getGameTime() + getParryTicks(spellLevel)));
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, .8f, 1.3f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 0.6 s, +0.1 s per level (the guard animation holds up to 0.9 s). */
    public int getParryTicks(int spellLevel) {
        return 10 + spellLevel * 2;
    }

    /** True while {@code fighter}'s guard is up. */
    public static boolean isGuarding(LivingEntity fighter) {
        Guard guard = GUARDS.get(fighter.getUUID());
        return guard != null && fighter.level().getGameTime() <= guard.until();
    }

    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        LivingEntity fighter = event.getEntity();
        Guard guard = GUARDS.get(fighter.getUUID());
        if (guard == null || fighter.level().isClientSide) {
            return;
        }
        if (fighter.level().getGameTime() > guard.until()) {
            GUARDS.remove(fighter.getUUID());
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (direct == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || isMagic(source)) {
            return;
        }
        boolean projectile = direct instanceof Projectile;
        if (!projectile && (direct != attacker || !(attacker instanceof LivingEntity))) {
            return;
        }
        Vec3 from = direct.position().subtract(fighter.position()).multiply(1, 0, 1);
        if (from.lengthSqr() > 1.0E-4 && from.normalize().dot(Combat.facing(fighter)) < Math.cos(Math.toRadians(GUARD_ANGLE))) {
            return;
        }
        event.setCanceled(true);
        GUARDS.remove(fighter.getUUID());
        Level level = fighter.level();
        Vec3 spark = fighter.getEyePosition().add(Combat.facing(fighter).scale(.6)).subtract(0, .3, 0);
        level.playSound(null, spark.x, spark.y, spark.z, SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, .6f, 1.9f);
        level.playSound(null, spark.x, spark.y, spark.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 1.2f);
        SpellUtil.particles(level, ParticleTypes.CRIT, spark.x, spark.y, spark.z, 14, .15, .15, .15, .4);
        if (projectile || !(attacker instanceof LivingEntity enemy)) {
            return;
        }
        enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, true, true));
        enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, true, true));
        if (fighter instanceof ServerPlayer player) {
            PhysicalAnimations.LONGSWORD_RIPOSTE.getForPlayer().ifPresent(animation -> ArcanesNetwork.playAnimation(player, animation));
        }
        LongswordParrySkill skill = (LongswordParrySkill) ModSpells.LONGSWORD_PARRY.get();
        SkillTimeline.after(fighter, RIPOSTE_TICK, f -> {
            StrikeTrail.spawn(level, f, StrikeTrail.Style.STAB, (float) RIPOSTE_REACH, 0, 0, false, StrikeTrail.STEEL);
            if (enemy.isAlive() && enemy.distanceToSqr(f) <= RIPOSTE_REACH * RIPOSTE_REACH) {
                Combat.hit(skill, f, enemy, skill.blow(guard.spellLevel(), f, enemy, .8f, 1.3f), .5);
                level.playSound(null, enemy.getX(), enemy.getY(), enemy.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.1f, 1.1f);
            }
        });
    }

    /** A spell's damage, except a fighting move's: a blade can turn a blow, not a fireball. */
    private static boolean isMagic(DamageSource source) {
        return source instanceof SpellDamageSource spellSource && !(spellSource.spell() instanceof PhysicalSkill);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        GUARDS.clear();
    }
}
