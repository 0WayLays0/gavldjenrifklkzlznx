package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.FlyingStack;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Flying Stack: pages fly out of the caster's book and pile up under their feet into a stack of paper that lifts them
 * off the ground; they ride it and fly where they look (see {@link FlyingStack}). Casting the spell again sends it
 * away (see {@link SummoningSpell}).
 */
@AutoSpellConfig
public class FlyingStackSpell extends SummoningSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("flying_stack");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(20)
            .build();

    public FlyingStackSpell() {
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 15;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getLifetime(spellLevel), 1)),
                Component.translatable("ui.arcanes_oublies.flight_speed", Utils.stringTruncation(getSpeed(spellLevel) * 20, 1)),
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
        return Optional.of(SoundEvents.BOOK_PAGE_TURN);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ENDER_DRAGON_FLAP);
    }

    /** While chanting, pages drift down and settle in a pile at the caster's feet. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .6f;
        double r = 1.1 - (entity.tickCount % 15) / 15.0 * .8;
        SpellUtil.particles(level, BookFx.paper(), entity.getX() + Mth.cos(a) * r, entity.getY() + .2 + r, entity.getZ() + Mth.sin(a) * r, 1, .02, .02, .02, 0);
        SpellUtil.particles(level, ParticleTypes.ENCHANT, entity.getX(), entity.getY() + .1, entity.getZ(), 2, .4, .05, .4, .3);
    }

    /** One stack per caster: a new one replaces the one still around. The caster climbs straight on. */
    @Override
    protected void summon(Level level, int spellLevel, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            dismissSummons(serverLevel, entity);
        }
        FlyingStack stack = ModEntities.FLYING_STACK.get().create(level);
        if (stack == null) {
            return;
        }
        entity.stopRiding();
        stack.moveTo(entity.getX(), entity.getY() + .1, entity.getZ(), entity.getYRot(), 0);
        stack.setup(entity, getLifetime(spellLevel), getHealth(spellLevel), getSpeed(spellLevel));
        level.addFreshEntity(stack);
        entity.startRiding(stack, true);
        // A little hop off the ground
        stack.setDeltaMovement(0, .35, 0);
        SpellUtil.particles(level, BookFx.paper(), stack.getX(), stack.getY() + .3, stack.getZ(), 24, .5, .2, .5, .2);
        SpellUtil.particles(level, ParticleTypes.ENCHANT, stack.getX(), stack.getY() + .3, stack.getZ(), 30, .5, .2, .5, 1);
    }

    /** 1 min at level 1, +30 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 600 + spellLevel * 600;
    }

    public double getHealth(int spellLevel) {
        return 16 + spellLevel * 6;
    }

    /** Top speed in blocks per tick: about 11 blocks per second at level 1, 16 at level 5. */
    public float getSpeed(int spellLevel) {
        return .5f + spellLevel * .06f;
    }
}
