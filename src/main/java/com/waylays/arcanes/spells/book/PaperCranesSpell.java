package com.waylays.arcanes.spells.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.PaperCrane;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Paper Cranes: glowing origami cranes fold themselves out of the caster's pages, flutter up around them, then dive
 * at the enemies in front and burst into paper confetti (see {@link PaperCrane}). The enemy the caster looks at gets
 * the first crane; the others share out the rest, nearest the line of sight first.
 */
@AutoSpellConfig
public class PaperCranesSpell extends AbstractSpell {
    public static final float SEEK_RADIUS = 18f;

    private final ResourceLocation spellId = ArcanesOublies.id("paper_cranes");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.BOOK_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(10)
            .build();

    public PaperCranesSpell() {
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 35;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.projectile_count", getCount(spellLevel))
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
        List<LivingEntity> targets = pickTargets(level, entity);
        int count = getCount(spellLevel);
        Vec3 look = entity.getLookAngle();
        for (int i = 0; i < count; i++) {
            LivingEntity target = targets.isEmpty() ? null : targets.get(i % targets.size());
            // Flutter out on a fan around the caster's shoulders
            float a = entity.getYRot() * Mth.DEG_TO_RAD + (i - (count - 1) * .5f) * .9f;
            Vec3 out = new Vec3(-Mth.sin(a), 0, Mth.cos(a));
            Vec3 start = entity.getEyePosition().add(out.scale(.6)).add(0, -.2, 0);
            PaperCrane crane = new PaperCrane(level, entity, getDamage(spellLevel, entity), target);
            crane.setPos(start.x, start.y, start.z);
            crane.shoot(out.scale(.6).add(look.scale(.2)).add(0, .45 + entity.getRandom().nextFloat() * .15, 0), .55f);
            level.addFreshEntity(crane);
            SpellUtil.particles(level, BookFx.paper(), start.x, start.y, start.z, 2, .1, .1, .1, .05);
        }
        SpellUtil.particles(level, ParticleTypes.ENCHANT, entity.getX(), entity.getEyeY(), entity.getZ(), 30, .4, .3, .4, 1);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, .5f, 2f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** The enemy in the crosshair first, then the others in front, nearest to the line of sight first. */
    private static List<LivingEntity> pickTargets(Level level, LivingEntity caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        HitResult hit = Utils.raycastForEntity(level, caster, 32, true, .35f);
        LivingEntity aimed = hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living
                && !SpellUtil.isAlly(caster, living) ? living : null;
        List<LivingEntity> targets = SpellUtil.enemiesAround(level, eye, SEEK_RADIUS, caster, e -> !(e instanceof ArmorStand)
                && e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) > .3 && caster.hasLineOfSight(e));
        targets.sort(Comparator.comparingDouble(e -> -e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look)));
        if (aimed != null) {
            targets.remove(aimed);
            targets.add(0, aimed);
        }
        return targets;
    }

    /** 3 cranes, +1 every 3 levels. */
    public int getCount(int spellLevel) {
        return 3 + (spellLevel - 1) / 3;
    }

    public float getDamage(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * .8f;
    }
}
