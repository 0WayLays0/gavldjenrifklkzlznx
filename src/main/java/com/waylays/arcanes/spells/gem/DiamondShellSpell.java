package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.DiamondShell;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Diamond Shell: twelve diamond plates round the caster take the blows and strike back (see {@link DiamondShell}). */
@AutoSpellConfig
public class DiamondShellSpell extends GemSpell {
    public DiamondShellSpell() {
        super("diamond_shell", SpellRarity.RARE, 6, 30, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 60;
    }

    public static int duration(int spellLevel) {
        return 240 + 40 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.shell_plates", DiamondShell.PLATES, (int) (DiamondShell.ABSORB * 100)),
                Component.translatable("ui.arcanes_oublies.shard_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(duration(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        DiamondShell.encase(level, entity, this, duration(spellLevel), getPower(spellLevel, entity));
        GemFx.sigil(level, SpellUtil.groundAt(level, entity.position(), 2, 1), Gem.DIAMOND, 2.4f, 24);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
