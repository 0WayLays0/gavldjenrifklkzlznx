package com.waylays.arcanes.spells.lava;

import com.waylays.arcanes.entity.lava.LavaScene;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.ElementSpell;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Lava Geyser: the ground cracks and glows, then a column of lava bursts up, flinging and burning those over it. */
@AutoSpellConfig
public class LavaGeyserSpell extends ElementSpell {
    public LavaGeyserSpell() {
        super("lava_geyser", ModSchools.LAVA_RESOURCE, SpellRarity.UNCOMMON, 6, 14, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power * 1.4f, 1)),
                Component.translatable("ui.arcanes_oublies.lava.lava_geyser", Utils.timeFromTicks(LavaScene.GEYSER_WARN, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            LavaScene.spawn(server, entity, this, LavaScene.Kind.GEYSER, SpellUtil.aimedSpot(level, entity, 24), null, entity.getYRot(),
                    LavaScene.GEYSER_WARN + LavaScene.GEYSER_BURST + 20, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
