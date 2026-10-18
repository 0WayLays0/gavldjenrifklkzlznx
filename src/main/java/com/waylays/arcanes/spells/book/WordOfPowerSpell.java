package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.WordOfPower;
import com.waylays.arcanes.registry.ModSchools;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Word of Power: the caster speaks a giant runic word; it takes shape in letters of fire in front of them and explodes
 * forward (see {@link WordOfPower}).
 */
@AutoSpellConfig
public class WordOfPowerSpell extends AbstractSpell {
    public static final float RANGE = 9f;
    public static final float CONE_DEGREES = 38f;
    /** How far in front of the caster's eyes the word is written. */
    public static final float DISTANCE = 2.6f;

    private final ResourceLocation spellId = ArcanesOublies.id("word_of_power");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(15)
            .build();

    public WordOfPowerSpell() {
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(RANGE, 1))
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
        return Optional.of(SoundEvents.EVOKER_PREPARE_ATTACK);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        // Written in front of the caster, closer if a wall is in the way
        Vec3 eye = entity.getEyePosition();
        Vec3 wanted = eye.add(entity.getLookAngle().scale(DISTANCE));
        Vec3 hit = level.clip(new ClipContext(eye, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getLocation();
        Vec3 at = hit.distanceTo(eye) < DISTANCE ? eye.add(hit.subtract(eye).scale(.8)) : wanted;
        WordOfPower word = new WordOfPower(level, entity, getDamage(spellLevel, entity));
        word.moveTo(at.x, at.y - .2, at.z, entity.getYRot(), entity.getXRot());
        level.addFreshEntity(word);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * 1.2f;
    }
}
