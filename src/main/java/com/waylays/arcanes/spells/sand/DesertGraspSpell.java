package com.waylays.arcanes.spells.sand;

import com.waylays.arcanes.entity.sand.SandLance;
import com.waylays.arcanes.entity.sand.SandScene;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Grasp of the Dunes: a giant hand of sand rises under the enemy the caster looks at (or where they point), holds it
 * fast, then closes its fist on it.
 */
@AutoSpellConfig
public class DesertGraspSpell extends SandSpell {
    public static final double RANGE = 18;
    public static final int LIFE = 70;

    public DesertGraspSpell() {
        super("desert_grasp", SpellRarity.UNCOMMON, 8, 12, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.hold_time", Utils.timeFromTicks(LIFE - SandScene.GRASP_CRUSH, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            LivingEntity target = aimed(level, entity, RANGE);
            Vec3 at = target != null ? target.position() : aimedSpot(level, entity, RANGE);
            SandScene hand = SandScene.spawn(server, entity, this, SandScene.Kind.GRASP, at, new Vector3f(0, 1, 0), 1.6f, LIFE, getPower(spellLevel, entity));
            if (target != null) {
                hand.follow(target);
            }
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 2f, .4f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
