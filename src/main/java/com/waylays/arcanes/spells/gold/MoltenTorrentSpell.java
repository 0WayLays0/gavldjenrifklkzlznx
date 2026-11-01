package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.MoltenGoldBlob;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Molten Torrent: hold to pour a stream of molten gold that burns what it hits and leaves glowing pools behind.
 */
@AutoSpellConfig
public class MoltenTorrentSpell extends AbstractSpell {
    private final ResourceLocation spellId = ArcanesOublies.id("molten_torrent");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GOLD_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(12)
            .build();

    public MoltenTorrentSpell() {
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 60;
        this.baseManaCost = 10;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2))
        );
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
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
        return Optional.empty();
    }

    /** Called when the stream starts, then every 10 ticks while it is held. */
    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, .6f, .5f + entity.getRandom().nextFloat() * .2f);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.LAVA_AMBIENT, SoundSource.PLAYERS, 1f, 1.2f);
        GoldFx.sigil(level, entity.position(), 1.3f, 40);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** Every tick while held: two gobs of molten gold. */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        RandomSource random = entity.getRandom();
        Vec3 look = entity.getLookAngle();
        Vec3 origin = entity.getEyePosition().add(look.scale(.7)).subtract(0, .35, 0);
        float damage = getDamage(spellLevel, entity);
        for (int i = 0; i < 2; i++) {
            Vec3 spread = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).scale(.07);
            MoltenGoldBlob blob = new MoltenGoldBlob(level, entity);
            blob.setPos(origin);
            blob.shoot(look.add(spread), .85f + random.nextFloat() * .2f);
            blob.setDamage(damage);
            level.addFreshEntity(blob);
        }
        SpellUtil.particles(level, GoldFx.MOLTEN_DUST, origin.x, origin.y, origin.z, 2, .05, .05, .05, .02);
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .5f;
    }
}
