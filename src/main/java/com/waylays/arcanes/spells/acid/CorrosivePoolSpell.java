package com.waylays.arcanes.spells.acid;

import com.waylays.arcanes.entity.acid.AcidScene;
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

import java.util.List;

/** Corrosive Pool: a gob of acid splashes where the caster points and spreads into a fuming pool that eats feet and armour. */
@AutoSpellConfig
public class CorrosivePoolSpell extends ElementSpell {
    public CorrosivePoolSpell() {
        super("corrosive_pool", ModSchools.ACID_RESOURCE, SpellRarity.UNCOMMON, 6, 14, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.arcanes_oublies.acid.corrosive_pool", Utils.stringTruncation(power * .3f, 1), Utils.timeFromTicks(AcidScene.POOL_TIME, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            AcidScene.spawn(server, entity, this, AcidScene.Kind.POOL, SpellUtil.aimedSpot(level, entity, 20), entity.getYRot(), 0, null, AcidScene.POOL_TIME,
                    getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
