package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.QuillStroke;
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
 * Quill of Fate: a giant spectral quill writes a line of ink on the ground ahead of the caster; the writing bursts
 * into rune pillars that fling enemies into the air (see {@link QuillStroke}). The line stops at walls.
 */
@AutoSpellConfig
public class QuillOfFateSpell extends AbstractSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("quill_of_fate");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(18)
            .build();

    public QuillOfFateSpell() {
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.length", Utils.stringTruncation(getLength(spellLevel), 1))
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
        return Optional.of(SoundEvents.VILLAGER_WORK_CARTOGRAPHER);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
        Vec3 dir = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 start = SpellUtil.groundAt(level, entity.position().add(dir.scale(1.2)), 2, 1);
        float length = getLength(spellLevel);
        // Stop at the first wall in the way
        Vec3 from = start.add(0, .5, 0);
        HitResult wall = level.clip(new ClipContext(from, from.add(dir.scale(length)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (wall.getType() != HitResult.Type.MISS) {
            length = (float) Math.max(1.5, wall.getLocation().distanceTo(from) - .3);
        }
        QuillStroke stroke = new QuillStroke(level, entity, getDamage(spellLevel, entity), length);
        stroke.moveTo(start.x, start.y, start.z, entity.getYRot(), 0);
        level.addFreshEntity(stroke);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getLength(int spellLevel) {
        return 9.5f + spellLevel * .5f;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 1.3f;
    }
}
