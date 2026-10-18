package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.FinalChapter;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * The Final Chapter (ultimate): a colossal grimoire opens above the caster, its pages form a dome over the area and
 * every enemy under it is marked with a rune that explodes (see {@link FinalChapter}).
 */
@AutoSpellConfig
public class FinalChapterSpell extends AbstractSpell {
    public static final float RADIUS = 10f;
    public static final float RUNE_SPLASH_RADIUS = 2.5f;
    public static final float HEIGHT = 8.5f;

    private final ResourceLocation spellId = ArcanesOublies.id("the_final_chapter");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(150)
            .build();

    public FinalChapterSpell() {
        this.manaCostPerLevel = 50;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 4;
        this.castTime = 40;
        this.baseManaCost = 280;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.rune_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1))
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
        return Optional.of(SoundEvents.BOOK_PAGE_TURN);
    }

    /** While reading the incantation: pages and glyphs circle the caster. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        float a = entity.tickCount * .3f;
        for (int i = 0; i < 2; i++) {
            float b = a + i * Mth.PI;
            double x = entity.getX() + Mth.cos(b) * 1.3;
            double z = entity.getZ() + Mth.sin(b) * 1.3;
            double y = entity.getY() + 1 + Mth.sin(a * .7f + i) * .5;
            SpellUtil.particles(level, BookFx.paper(), x, y, z, 1, 0, 0, 0, 0);
            SpellUtil.particles(level, ParticleTypes.ENCHANT, x, y, z, 2, .1, .1, .1, .3);
        }
        SpellUtil.particles(level, BookFx.VIOLET_DUST, entity.getX(), entity.getY() + 2.5, entity.getZ(), 2, .4, .2, .4, 0);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 ground = SpellUtil.groundAt(level, entity.position(), 6, 1);
        float height = HEIGHT;
        Vec3 above = ground.add(0, 1, 0);
        HitResult roof = level.clip(new ClipContext(above, above.add(0, HEIGHT + 1, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (roof.getType() != HitResult.Type.MISS) {
            height = (float) Math.max(4, roof.getLocation().y - ground.y - 1.2);
        }
        FinalChapter chapter = new FinalChapter(level, entity, getDamage(spellLevel, entity), height);
        chapter.moveTo(ground.x, ground.y + height, ground.z, entity.getYRot(), 0);
        level.addFreshEntity(chapter);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 1.8f;
    }
}
