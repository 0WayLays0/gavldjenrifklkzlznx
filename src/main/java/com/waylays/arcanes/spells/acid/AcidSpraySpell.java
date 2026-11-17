package com.waylays.arcanes.spells.acid;

import com.waylays.arcanes.entity.acid.AcidScene;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.ElementSpell;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/** Acid Spray: a fan of acid sprayed from the hand, burning and sundering all it splashes, eating the armour they wear. */
@AutoSpellConfig
public class AcidSpraySpell extends ElementSpell {
    public AcidSpraySpell() {
        super("acid_spray", ModSchools.ACID_RESOURCE, SpellRarity.COMMON, 8, 5, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power * .8f, 1)),
                Component.translatable("ui.arcanes_oublies.acid.acid_spray", Utils.stringTruncation(AcidScene.SPRAY_REACH, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            AcidScene.spawn(server, entity, this, AcidScene.Kind.SPRAY, entity.getEyePosition().add(0, -.3, 0), entity.getYRot(), entity.getXRot(), null,
                    AcidScene.SPRAY_TIME + 14, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
