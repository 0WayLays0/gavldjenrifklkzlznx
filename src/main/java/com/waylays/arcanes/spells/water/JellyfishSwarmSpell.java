package com.waylays.arcanes.spells.water;

import com.waylays.arcanes.entity.water.Jellyfish;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Jellyfish Swarm: glowing jellyfish of water swim off after the nearest enemies and sting them (see Jellyfish). */
@AutoSpellConfig
public class JellyfishSwarmSpell extends WaterSpell {
    public JellyfishSwarmSpell() {
        super("jellyfish_swarm", SpellRarity.RARE, 6, 30, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public static int count(int spellLevel) {
        return 3 + spellLevel / 2;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.summon_count", count(spellLevel)),
                Component.translatable("ui.arcanes_oublies.sting_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1), Jellyfish.STINGS),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(300, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int n = count(spellLevel);
        for (int k = 0; k < n; k++) {
            float a = entity.getYRot() * Mth.DEG_TO_RAD + k * Mth.TWO_PI / n;
            Vec3 at = entity.position().add(-Mth.sin(a) * 1.6, 2 + .3 * (k % 2), Mth.cos(a) * 1.6);
            Jellyfish.release(level, entity, this, at, k, 300, getPower(spellLevel, entity));
        }
        WaterFx.sound(level, entity.position(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.6f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
