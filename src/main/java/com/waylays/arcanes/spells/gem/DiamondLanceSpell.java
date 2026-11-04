package com.waylays.arcanes.spells.gem;

import com.waylays.arcanes.entity.gem.GemScene;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Diamond Lance: a long point of diamond shot down the caster's gaze faster than the eye: it goes through every enemy
 * on its line, a beam behind it fanning into the colours of the rainbow, and breaks where it stops.
 */
@AutoSpellConfig
public class DiamondLanceSpell extends GemSpell {
    public DiamondLanceSpell() {
        super("diamond_lance", SpellRarity.UNCOMMON, 8, 10, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 8;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.pierce_line"),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(GemScene.LANCE_RANGE, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 from = entity.getEyePosition().add(0, -.15, 0);
            GemScene.spawn(server, entity, this, GemScene.Kind.LANCE, from, entity.getLookAngle().toVector3f(), GemScene.LANCE_RANGE, 16,
                    getPower(spellLevel, entity));
            GemFx.sigil(level, SpellUtil.groundAt(level, entity.position(), 2, 1), Gem.DIAMOND, 1.6f, 14);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
