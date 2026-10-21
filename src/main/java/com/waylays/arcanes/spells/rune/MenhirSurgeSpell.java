package com.waylays.arcanes.spells.rune;

import com.waylays.arcanes.entity.rune.Runestone;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Menhir Surge: the caster strikes the ground and a line of rune-carved menhirs bursts out of it one after another,
 * straight ahead, throwing up and hurting whatever stands on them. A wall or a cliff stops the line.
 */
@AutoSpellConfig
public class MenhirSurgeSpell extends RuneSpell {
    public static final double SPACING = 1.7;
    /** Ticks between two menhirs. */
    public static final int STEP = 3;

    public MenhirSurgeSpell() {
        super("menhir_surge", SpellRarity.EPIC, 3, 18, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public static int menhirs(int spellLevel) {
        return 6 + spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.menhirs", menhirs(spellLevel)),
                Component.translatable("ui.arcanes_oublies.launch"));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.DEEPSLATE_BREAK);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        float yaw = entity.getYRot();
        Vec3 dir = direction(yaw);
        Vec3 side = new Vec3(dir.z, 0, -dir.x);
        float damage = getPower(spellLevel, entity);
        int now = (int) level.getGameTime();
        double lastY = entity.getY();
        for (int i = 0; i < menhirs(spellLevel); i++) {
            Vec3 p = entity.position().add(dir.scale(2 + i * SPACING)).add(side.scale((level.random.nextDouble() - .5) * .5));
            Vec3 ground = SpellUtil.groundAt(level, new Vec3(p.x, lastY, p.z), 3, 2);
            if (Math.abs(ground.y - lastY) > 2.5) {
                break;
            }
            lastY = ground.y;
            Runestone.menhir(level, entity, ground, now + 4 + i * STEP, .85f + i * .06f, damage);
        }
        RuneFx.sound(level, entity.position(), SoundEvents.DEEPSLATE_BREAK, 1.6f, .5f);
        SpellUtil.cameraShake(level, entity.position(), 10, 10);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
