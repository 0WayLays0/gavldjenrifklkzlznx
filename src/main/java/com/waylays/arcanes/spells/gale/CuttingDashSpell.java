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

/** Cutting Dash: the caster darts ahead in a rush of air, cutting whoever is beside the way. */
@AutoSpellConfig
public class CuttingDashSpell extends ElementSpell {
    public CuttingDashSpell() {
        super("cutting_dash", ModSchools.GALE_RESOURCE, SpellRarity.UNCOMMON, 8, 7, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.arcanes_oublies.gale.cutting_dash", Math.round(GaleScene.DASH_REACH)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 look = entity.getLookAngle();
            Vec3 aim = new Vec3(look.x, Math.max(-.3, Math.min(.3, look.y)), look.z);
            GaleScene.spawn(server, entity, this, GaleScene.Kind.DASH, entity.position(), aim, 0, GaleScene.DASH_TIME + GaleScene.DASH_AFTER,
                    getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
