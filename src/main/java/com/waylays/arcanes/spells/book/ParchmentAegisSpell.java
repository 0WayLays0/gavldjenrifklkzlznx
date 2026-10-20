package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.ParchmentAegisEffect;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Parchment Aegis: pages rise and circle the caster as armour; each one stops a projectile or softens a blow and
 * strikes back (see {@link ParchmentAegisEffect}).
 */
@AutoSpellConfig
public class ParchmentAegisSpell extends AbstractSpell {
    /** For an aegis given without the spell (/effect). */
    public static final int FALLBACK_PAGES = 4;
    public static final float FALLBACK_DAMAGE = 4f;

    private final ResourceLocation spellId = ArcanesOublies.id("parchment_aegis");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(6)
            .setCooldownSeconds(35)
            .build();

    public ParchmentAegisSpell() {
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.pages", getPages(spellLevel)),
                Component.translatable("ui.irons_spellbooks.damage_reduction", (int) (ParchmentAegisEffect.absorbRatio(spellLevel - 1) * 100)),
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(spellLevel), 1))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
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
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ARMOR_EQUIP_LEATHER.value());
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        ParchmentAegisEffect.shield(entity, spellLevel, getDuration(spellLevel), getPages(spellLevel), getDamage(spellLevel, entity));
        double h = entity.getBbHeight();
        SpellUtil.particles(level, BookFx.paper(), entity.getX(), entity.getY() + h * .3, entity.getZ(), 16, .6, .2, .6, .1);
        SpellUtil.particles(level, ParticleTypes.ENCHANT, entity.getX(), entity.getY() + h * .5, entity.getZ(), 30, .5, .4, .5, 1);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 2f, .8f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 3 pages, +1 every 2 levels. */
    public int getPages(int spellLevel) {
        return 3 + spellLevel / 2;
    }

    /** 20 s, +2 s per level. */
    public int getDuration(int spellLevel) {
        return 380 + spellLevel * 40;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .8f;
    }
}
