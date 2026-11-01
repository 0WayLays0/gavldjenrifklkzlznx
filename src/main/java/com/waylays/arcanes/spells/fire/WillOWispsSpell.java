package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.entity.fire.Wisp;
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
import net.minecraft.sounds.SoundEvents;
import java.util.List;

/** Will-o'-Wisps: little living flames circle the caster and dart at the enemies that come near (see Wisp). */
@AutoSpellConfig
public class WillOWispsSpell extends StarfireSpell {
    public static final int LIFE = 400;

    public WillOWispsSpell(int tier) {
        super("will_o_wisps", tier, SpellRarity.RARE, 6, 20, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 8;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 50;
        costsByHeat();
    }

    public static int count(int spellLevel) {
        return 3 + spellLevel / 2;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                heatLine(caster),
                Component.translatable("ui.irons_spellbooks.summon_count", count(spellLevel)),
                Component.translatable("ui.arcanes_oublies.wisp_damage", Utils.stringTruncation(hot(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int tier = heat(entity);
        for (int k = 0; k < count(spellLevel); k++) {
            Wisp.kindle(level, entity, this, k, LIFE, hot(spellLevel, entity), tier);
        }
        FireFx.sound(level, entity.position(), SoundEvents.BLAZE_AMBIENT, 1, 1.6f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
