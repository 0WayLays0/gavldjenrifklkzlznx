package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.FlyingGrimoire;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.SummoningSpell;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Forbidden Library: flying grimoires tear themselves out of a rune circle around the caster and fight for them for
 * a while, biting and spitting small spells (see {@link FlyingGrimoire}). Casting the spell again sends them away
 * (see {@link SummoningSpell}).
 */
@AutoSpellConfig
public class ForbiddenLibrarySpell extends SummoningSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("forbidden_library");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(70)
            .build();

    public ForbiddenLibrarySpell() {
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 30;
        this.baseManaCost = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.summon_count", getCount(spellLevel)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getLifetime(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getBiteDamage(spellLevel, caster), 1))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.EVOKER_PREPARE_SUMMON);
    }

    /** While reading: pages whirl up from a circle on the ground. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .35f;
        for (int i = 0; i < 3; i++) {
            float b = a + i * Mth.TWO_PI / 3;
            double x = entity.getX() + Mth.cos(b) * 1.8;
            double z = entity.getZ() + Mth.sin(b) * 1.8;
            SpellUtil.particles(level, BookFx.paper(), x, entity.getY() + .2, z, 1, .05, .1, .05, .05);
            SpellUtil.particles(level, ParticleTypes.ENCHANT, x, entity.getY() + .5, z, 2, .1, .3, .1, .5);
        }
    }

    @Override
    protected boolean summon(Level level, int spellLevel, LivingEntity entity) {
        int count = getCount(spellLevel);
        boolean summoned = false;
        Vec3 eye = entity.getEyePosition();
        for (int i = 0; i < count; i++) {
            float a = entity.getYRot() * Mth.DEG_TO_RAD + (i + .5f) * Mth.TWO_PI / count;
            Vec3 wanted = entity.position().add(-Mth.sin(a) * 1.6, entity.getBbHeight() + .2, Mth.cos(a) * 1.6);
            // Not inside a wall or under the ceiling: as far out as there is room
            Vec3 spot = level.clip(new ClipContext(eye, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getLocation();
            spot = spot.add(eye.subtract(spot).normalize().scale(.4)).subtract(0, .25, 0);
            FlyingGrimoire grimoire = ModEntities.FLYING_GRIMOIRE.get().create(level);
            if (grimoire == null) {
                continue;
            }
            grimoire.moveTo(spot.x, spot.y, spot.z, entity.getYRot(), 0);
            grimoire.setup(entity, getLifetime(spellLevel), getHealth(spellLevel), getBiteDamage(spellLevel, entity), getBoltDamage(spellLevel, entity));
            if (entity instanceof Mob mob && mob.getTarget() != null) {
                grimoire.setTarget(mob.getTarget());
            }
            summoned |= level.addFreshEntity(grimoire);
            SpellUtil.particles(level, BookFx.paper(), spot.x, spot.y, spot.z, 10, .2, .2, .2, .15);
            SpellUtil.particles(level, ParticleTypes.ENCHANT, spot.x, spot.y, spot.z, 25, .2, .2, .2, 1);
            SpellUtil.particles(level, BookFx.VIOLET_DUST, spot.x, spot.y, spot.z, 12, .3, .3, .3, 0);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.5f, .7f + i * .15f);
        }
        SpellUtil.particles(level, new BlastwaveParticleOptions(BookFx.VIOLET, 3.5f), entity.getX(), entity.getY() + .1, entity.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.5f, .8f);
        return summoned;
    }

    /** 2 grimoires, 3 from level 2, 4 from level 4. */
    public int getCount(int spellLevel) {
        return Math.min(4, 2 + spellLevel / 2);
    }

    /** 20 s at level 1, +4 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 320 + spellLevel * 80;
    }

    public double getHealth(int spellLevel) {
        return 10 + spellLevel * 2;
    }

    public float getBiteDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }

    public float getBoltDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .6f;
    }
}
