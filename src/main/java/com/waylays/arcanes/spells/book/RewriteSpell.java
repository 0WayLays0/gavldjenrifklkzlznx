package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.RewriteAura;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rewrite: the caster rewrites their own story. Every harmful effect on them is erased (and the flames put out),
 * then they heal a little; script winds around them meanwhile (see {@link RewriteAura}).
 */
@AutoSpellConfig
public class RewriteSpell extends AbstractSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("rewrite");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(40)
            .build();

    public RewriteSpell() {
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.healing", Utils.stringTruncation(getHealing(spellLevel, caster), 1)));
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
        return Optional.of(SoundEvents.VILLAGER_WORK_CARTOGRAPHER);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        // Erase the bad chapters: harmful effects come off as ink
        List<MobEffect> erased = new ArrayList<>();
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            if (instance.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                erased.add(instance.getEffect());
            }
        }
        for (MobEffect effect : erased) {
            entity.removeEffect(effect);
        }
        entity.clearFire();
        double h = entity.getBbHeight();
        if (!erased.isEmpty()) {
            SpellUtil.particles(level, BookFx.INK_DUST, entity.getX(), entity.getY() + h * .5, entity.getZ(), 12 * erased.size(), .4, h * .35, .4, .05);
            SpellUtil.particles(level, ParticleTypes.SQUID_INK, entity.getX(), entity.getY() + h * .5, entity.getZ(), 4 * erased.size(), .3, h * .3, .3, .05);
        }
        float heal = getHealing(spellLevel, entity);
        MinecraftForge.EVENT_BUS.post(new SpellHealEvent(entity, entity, heal, getSchoolType()));
        entity.heal(heal);
        level.addFreshEntity(new RewriteAura(level, entity));
        SpellUtil.particles(level, BookFx.GILT_DUST, entity.getX(), entity.getY() + h * .5, entity.getZ(), 20, .4, h * .4, .4, 0);
        SpellUtil.particles(level, ParticleTypes.ENCHANT, entity.getX(), entity.getY() + h, entity.getZ(), 40, .3, .4, .3, 1.2);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.5f, .9f);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.2f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getHealing(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 1.5f;
    }
}
