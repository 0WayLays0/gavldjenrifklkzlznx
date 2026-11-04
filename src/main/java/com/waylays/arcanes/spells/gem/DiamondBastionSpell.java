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
 * Diamond Bastion: a great cluster of diamond bursts out of the ground at the caster's feet for twelve seconds, a dome of
 * pale light over it: the caster's side within it is hardened (Resistance II) and enemy shots flying into it are
 * turned aside. When its time runs out it shatters, its crystals striking the enemies round it.
 */
@AutoSpellConfig
public class DiamondBastionSpell extends GemSpell {
    public static final float RADIUS = 5;
    public static final int LIFE = 240 + GemScene.BASTION_SHATTER;

    public DiamondBastionSpell() {
        super("diamond_bastion", SpellRarity.EPIC, 5, 45, CastType.INSTANT, SpellAnimations.TOUCH_GROUND_ANIMATION);
        this.manaCostPerLevel = 12;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
        this.castTime = 0;
        this.baseManaCost = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.ward_resistance", "II"),
                Component.translatable("ui.arcanes_oublies.mirror_turns"),
                Component.translatable("ui.arcanes_oublies.shatter_damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - GemScene.BASTION_SHATTER, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 at = SpellUtil.groundAt(level, entity.position(), 2, 1);
            GemScene.spawn(server, entity, this, GemScene.Kind.BASTION, at, new Vector3f(0, 1, 0), RADIUS, LIFE, getPower(spellLevel, entity));
            GemFx.sigil(level, at, Gem.DIAMOND, RADIUS, 30);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
