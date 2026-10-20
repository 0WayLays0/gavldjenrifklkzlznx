package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.entity.boss.RuneWall;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Rune Prison: a ring is marked around the target, warning where a wall is about to rise; whoever is still inside it
 * when the runes stand up is shut in with whatever the caster has in store (see {@link RuneWall}). The arena lasts a
 * few seconds and turns the battlefield into a closed ring.
 */
@AutoSpellConfig
public class RunePrisonSpell extends BossSpell {
    public static final double RANGE = 24;
    public static final float RADIUS = 9.25f;
    public static final float RING = 1.5f;

    public RunePrisonSpell() {
        super("rune_prison", SpellRarity.EPIC, 3, 40, BossAnimations.PRISON);
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 80;
    }

    public static int duration(int spellLevel) {
        return 160 + 40 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(duration(spellLevel), 1)),
                warningInfo()
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ILLUSIONER_PREPARE_MIRROR);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 c = aimedGround(level, entity, RANGE);
        BossTelegraph.ring(level, entity, this, spellLevel, c, RADIUS - RING * .5f, RADIUS + RING * .5f)
                .style(BossTelegraph.Style.VOID).damage(getDamage(spellLevel, entity)).part("seal").spawn();
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onTelegraphFire(BossTelegraph t, ServerLevel level) {
        Vec3 c = t.position();
        // Caught in the ring itself: hurt and thrown out of the way of the rising wall, to the nearer side
        for (LivingEntity target : t.targetsInside()) {
            t.strike(target, t.getDamage());
            double d = target.position().subtract(c).horizontalDistance();
            knockAway(target, c, d < RADIUS ? -.8 : .8, .3);
        }
        RuneWall.spawn(level, t.getLivingOwner(), this, c, RADIUS, duration(t.getSpellLevel()), t.getDamage() * .6f);
        BossFx.shockwave(level, c, RADIUS * 2.1f, t.getStyle().rune);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 3f, .5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.HOSTILE, 3f, .6f);
    }
}
