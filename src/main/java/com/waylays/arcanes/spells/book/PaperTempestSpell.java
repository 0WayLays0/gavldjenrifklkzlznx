package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.PaperTempest;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
 * Paper Tempest: the caster tears every page out of their book at once and a tornado of paper rises where they look
 * (see {@link PaperTempest}). It follows their eyes while it lasts, dragging enemies into the funnel.
 */
@AutoSpellConfig
public class PaperTempestSpell extends AbstractSpell {
    public static final double RANGE = 24;

    private final ResourceLocation spellId = ArcanesOublies.id("paper_tempest");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(6)
            .setCooldownSeconds(35)
            .build();

    public PaperTempestSpell() {
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 30;
        this.baseManaCost = 60;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(spellLevel), 1))
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

    /** While chanting, pages peel off the book and whirl faster and faster around the caster. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .5f;
        for (int i = 0; i < 2; i++) {
            float b = a + i * Mth.PI;
            double r = 1.2;
            SpellUtil.particles(level, BookFx.paper(), entity.getX() + Mth.cos(b) * r, entity.getY() + .4 + (entity.tickCount % 15) / 10.0, entity.getZ() + Mth.sin(b) * r, 1, .05, .05, .05, .02);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 at;
        if (entity instanceof Mob mob && mob.getTarget() != null) {
            at = mob.getTarget().position();
        } else {
            at = Utils.getTargetBlock(level, entity, ClipContext.Fluid.NONE, RANGE).getLocation();
        }
        at = SpellUtil.groundAt(level, at.add(0, .5, 0), 6, 1);
        level.addFreshEntity(new PaperTempest(level, entity, at, getRadius(spellLevel), getDuration(spellLevel), getDamage(spellLevel, entity)));
        SpellUtil.particles(level, ParticleTypes.ENCHANT, entity.getX(), entity.getEyeY(), entity.getZ(), 40, .4, .4, .4, 1.2);
        SpellUtil.particles(level, BookFx.paper(), entity.getX(), entity.getEyeY(), entity.getZ(), 12, .3, .3, .3, .3);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** Damage of each cut (twice a second); the final gust deals double. */
    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .5f;
    }

    public float getRadius(int spellLevel) {
        return 3.5f + spellLevel * .25f;
    }

    /** 6 s, +1 s per level. */
    public int getDuration(int spellLevel) {
        return 100 + spellLevel * 20;
    }
}
