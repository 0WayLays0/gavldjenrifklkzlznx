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

/** Air Slash: a blade of air, so dense it bends the light, flies ahead and cuts all it passes through. */
@AutoSpellConfig
public class AirSlashSpell extends ElementSpell {
    public AirSlashSpell() {
        super("air_slash", ModSchools.GALE_RESOURCE, SpellRarity.COMMON, 10, 2, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = getPower(spellLevel, caster);
        return List.of(Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.arcanes_oublies.gale.air_slash", Math.round(GaleScene.SLASH_SPEED * GaleScene.SLASH_TIME)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            // Each blade slanted its own way: flat, or tilted to one side or the other
            float[] tilts = {0, .45f, -.45f, .9f, -.9f};
            float tilt = tilts[entity.getRandom().nextInt(tilts.length)];
            GaleScene.spawn(server, entity, this, GaleScene.Kind.SLASH, entity.getEyePosition().add(0, -.25, 0).add(entity.getLookAngle().scale(.8)),
                    entity.getLookAngle(), tilt, GaleScene.SLASH_TIME + 10, getPower(spellLevel, entity));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
