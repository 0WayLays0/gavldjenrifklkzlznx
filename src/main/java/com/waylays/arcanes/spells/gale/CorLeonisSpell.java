package com.waylays.arcanes.spells.gale;

import com.waylays.arcanes.entity.gale.GaleScene;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Cor Leonis (the old name of the lion's heart star): for a while blades of air cut endlessly all round the caster, who runs faster, a storm of blades in human form. */
@AutoSpellConfig
public class CorLeonisSpell extends ElementSpell {
    public CorLeonisSpell() {
        super("cor_leonis", ModSchools.GALE_RESOURCE, SpellRarity.LEGENDARY, 3, 90, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 120;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.arcanes_oublies.gale.cor_leonis", Utils.timeFromTicks(GaleScene.STORM_TIME + spellLevel * 40, 1),
                        Utils.stringTruncation(GaleScene.STORM_R, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            GaleScene.spawn(server, entity, this, GaleScene.Kind.STORM, entity.position(), facing(entity), 0, GaleScene.STORM_TIME + spellLevel * 40,
                    getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
