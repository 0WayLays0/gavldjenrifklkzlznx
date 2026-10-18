package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.RazorPage;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Razor Pages: a fan of pages with glowing razor edges flies from the caster's hands and slices through enemies
 * (see {@link RazorPage}).
 */
@AutoSpellConfig
public class RazorPagesSpell extends AbstractSpell {
    public static final float SPEED = 1.7f;

    private final ResourceLocation spellId = ArcanesOublies.id("razor_pages");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(4)
            .build();

    public RazorPagesSpell() {
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.projectile_count", getCount(spellLevel)),
                Component.translatable("ui.arcanes_oublies.pierce", getPierce(spellLevel))
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
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int count = getCount(spellLevel);
        float spread = 8f + 4f * count;
        Vec3 start = entity.getEyePosition().add(0, -.25, 0).add(entity.getLookAngle().scale(.6));
        for (int i = 0; i < count; i++) {
            // An even fan across the caster's view, a little uneven in speed so the pages do not fly as a wall
            float offset = count == 1 ? 0 : -spread / 2 + spread * i / (count - 1);
            Vec3 dir = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot() + offset);
            RazorPage page = new RazorPage(level, entity, getDamage(spellLevel, entity), getPierce(spellLevel), this);
            page.setPos(start.x, start.y, start.z);
            page.shoot(dir, SPEED * (.92f + .16f * entity.getRandom().nextFloat()));
            level.addFreshEntity(page);
        }
        SpellUtil.particles(level, ParticleTypes.ENCHANT, start.x, start.y, start.z, 20, .2, .2, .2, .8);
        SpellUtil.particles(level, BookFx.paper(), start.x, start.y, start.z, 4, .15, .15, .15, .1);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.8f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** 3 pages, +1 every 3 levels. */
    public int getCount(int spellLevel) {
        return 3 + (spellLevel - 1) / 3;
    }

    /** Enemies a page cuts through after the first one. */
    public int getPierce(int spellLevel) {
        return spellLevel >= 6 ? 2 : 1;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .6f;
    }
}
