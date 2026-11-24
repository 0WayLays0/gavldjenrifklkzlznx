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

/** Crushing Gravity: the air over the spot looked at weighs down; those under it are bowed, crouched under the weight, slowed and unable to jump. */
@AutoSpellConfig
public class CrushingGravitySpell extends ElementSpell {
    public CrushingGravitySpell() {
        super("crushing_gravity", ModSchools.GALE_RESOURCE, SpellRarity.RARE, 6, 20, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power * .25f, 1)),
                Component.translatable("ui.arcanes_oublies.gale.crushing_gravity", Utils.stringTruncation(GaleScene.GRAVITY_R, 1),
                        Utils.timeFromTicks(GaleScene.GRAVITY_TIME + spellLevel * 10, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = SpellUtil.aimedSpot(level, entity, 22);
            GaleScene.spawn(server, entity, this, GaleScene.Kind.GRAVITY, at, facing(entity), 0, GaleScene.GRAVITY_TIME + spellLevel * 10,
                    getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
