package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.PageStorm;
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
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Page Storm: a whirlwind of runic pages wraps around the target and slashes it again and again (see {@link PageStorm}).
 */
@AutoSpellConfig
public class PageStormSpell extends AbstractSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("page_storm");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(12)
            .build();

    public PageStormSpell() {
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.cut_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(getDuration(spellLevel), 1))
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
        return Optional.of(SoundEvents.BOOK_PAGE_TURN);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 24, .35f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            LivingEntity target = targetData.getTarget((ServerLevel) level);
            if (target != null) {
                level.addFreshEntity(new PageStorm(level, entity, target, getDamage(spellLevel, entity), getDuration(spellLevel), getPages(spellLevel)));
                // A stream of pages leaves the caster's hands for the target
                Vec3 from = entity.getEyePosition().add(entity.getLookAngle().scale(.8));
                Vec3 to = target.getBoundingBox().getCenter();
                Vec3 delta = to.subtract(from);
                int steps = (int) (delta.length() * 2);
                for (int i = 0; i <= steps; i++) {
                    Vec3 p = from.add(delta.scale(i / (double) Math.max(1, steps)));
                    SpellUtil.particles(level, i % 3 == 0 ? BookFx.paper() : ParticleTypes.ENCHANT, p.x, p.y, p.z, 1, .1, .1, .1, .02);
                }
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 2.7 s at level 1, +0.2 s per level. */
    public int getDuration(int spellLevel) {
        return 50 + spellLevel * 4;
    }

    public int getPages(int spellLevel) {
        return 8 + spellLevel;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .4f;
    }
}
