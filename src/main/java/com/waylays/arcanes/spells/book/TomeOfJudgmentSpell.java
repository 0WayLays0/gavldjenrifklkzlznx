package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.TomeOfJudgment;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Tome of Judgment: a giant book opens in the sky over the targeted spot and closes on it, crushing the enemies
 * caught between its pages (see {@link TomeOfJudgment}).
 */
@AutoSpellConfig
public class TomeOfJudgmentSpell extends AbstractSpell {
    public static final float RANGE = 24f;

    private final ResourceLocation spellId = ArcanesOublies.id("tome_of_judgment");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(35)
            .build();

    public TomeOfJudgmentSpell() {
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 3;
        this.castTime = 20;
        this.baseManaCost = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.cast_range", Utils.stringTruncation(RANGE, 1))
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
        return Optional.of(SoundEvents.EVOKER_CAST_SPELL);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        // A glyph rises from the caster's raised hand toward the sky
        SpellUtil.particles(level, ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 2.4, entity.getZ(), 4, .2, .2, .2, .6);
        SpellUtil.particles(level, BookFx.VIOLET_DUST, entity.getX(), entity.getY() + 2.6 + entity.getRandom().nextFloat(), entity.getZ(), 1, .05, .3, .05, 0);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        HitResult hit = Utils.raycastForEntity(level, entity, RANGE, true, .35f);
        Vec3 at = hit instanceof EntityHitResult entityHit ? entityHit.getEntity().position() : hit.getLocation();
        Vec3 ground = SpellUtil.groundAt(level, at, 5, 1);
        TomeOfJudgment tome = new TomeOfJudgment(level, entity, getDamage(spellLevel, entity));
        tome.moveTo(ground.x, ground.y, ground.z, entity.getYRot(), 0);
        level.addFreshEntity(tome);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 1.6f;
    }
}
