package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.SummoningSpell;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * An Errethys summoning: while the caster chants, motes of mana spiral around them; then a single creature takes shape
 * in front of them (on the ground, or in the air for flyers and swimmers) and fights for them for a while. Casting the
 * spell again sends it away (see {@link SummoningSpell}).
 */
public abstract class ManaSummonSpell extends SummoningSpell {
    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;

    protected ManaSummonSpell(String name, SpellRarity minRarity, int maxLevel, int cooldownSeconds) {
        this.spellId = ArcanesOublies.id(name);
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(minRarity)
                .setSchoolResource(ModSchools.ERRETHYS_RESOURCE)
                .setMaxLevel(maxLevel)
                .setCooldownSeconds(cooldownSeconds)
                .build();
    }

    protected abstract EntityType<? extends ManaSummon> summonType();

    public abstract double getHealth(int spellLevel);

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }

    /** Flyers and swimmers appear around the caster's shoulders instead of on the ground. */
    protected boolean airborne() {
        return false;
    }

    /** Extra set-up of each creature after {@link ManaSummon#setup}. */
    protected void configure(ManaSummon summon, int spellLevel) {
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getLifetime(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.hp", (int) getHealth(spellLevel))
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
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.EVOKER_CAST_SPELL);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .35f;
        for (int i = 0; i < 2; i++) {
            float b = a + i * Mth.PI;
            double r = 1.4 + Mth.sin(a * .5f) * .2;
            double y = entity.getY() + .3 + (entity.tickCount % 20) / 20.0 * entity.getBbHeight();
            SpellUtil.particles(level, i == 0 ? ErrethysFx.SKY_DUST : ErrethysFx.PALE_DUST, entity.getX() + Mth.cos(b) * r, y, entity.getZ() + Mth.sin(b) * r, 1, .02, .02, .02, 0);
        }
    }

    /** One creature per summoner and spell: a new one replaces the one still around (from a scroll, say). */
    @Override
    protected void summon(Level level, int spellLevel, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            dismissSummons(serverLevel, entity);
        }
        // Right in front of the caster
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        Vec3 ahead = entity.position().add(-Mth.sin(yaw) * 1.8, 0, Mth.cos(yaw) * 1.8);
        Vec3 spot;
        if (airborne()) {
            // As far out as there is room, never inside a wall or the ceiling
            Vec3 eye = entity.getEyePosition();
            Vec3 wanted = ahead.add(0, entity.getBbHeight() * .8, 0);
            spot = level.clip(new ClipContext(eye, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getLocation();
            spot = spot.add(eye.subtract(spot).normalize().scale(.4));
        } else {
            spot = SpellUtil.groundAt(level, ahead, 3, 1);
        }
        ManaSummon summon = summonType().create(level);
        if (summon == null) {
            return;
        }
        summon.moveTo(spot.x, spot.y, spot.z, entity.getYRot(), 0);
        if (!level.noCollision(summon)) {
            // No room there: take shape at the caster's side
            summon.moveTo(entity.getX(), entity.getY() + (airborne() ? entity.getBbHeight() * .5 : 0), entity.getZ(), entity.getYRot(), 0);
        }
        summon.setup(entity, getLifetime(spellLevel), getHealth(spellLevel), getDamage(spellLevel, entity));
        configure(summon, spellLevel);
        if (entity instanceof Mob mob && mob.getTarget() != null) {
            summon.setTarget(mob.getTarget());
        }
        level.addFreshEntity(summon);
        ErrethysFx.materialize(level, summon.position(), Math.max(.7f, summon.getBbWidth() * 1.6f));
    }
}
