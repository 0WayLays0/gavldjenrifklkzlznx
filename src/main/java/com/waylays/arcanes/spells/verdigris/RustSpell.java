package com.waylays.arcanes.spells.verdigris;

import com.waylays.arcanes.entity.verdigris.RustBolt;
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

/** Rust: a clot of rust flakes hurled at an enemy rusts its armour (it protects less and wears away). */
@AutoSpellConfig
public class RustSpell extends VerdigrisSpell {
    public static final float SPEED = 1.3f;

    public RustSpell() {
        super("rust_bolt", SpellRarity.UNCOMMON, 5, 10, CastType.INSTANT, SpellAnimations.ANIMATION_INSTANT_CAST);
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 25;
    }

    public static int rustTicks(int spellLevel) {
        return 160 + 40 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.arcanes_oublies.rust_armor", 15 * spellLevel),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(rustTicks(spellLevel), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 from = entity.getEyePosition().add(entity.getLookAngle().scale(.6)).add(0, -.25, 0);
        RustBolt.shoot(level, entity, this, from, entity.getLookAngle(), SPEED, getPower(spellLevel, entity), rustTicks(spellLevel), spellLevel - 1);
        VerdigrisFx.rust(level, from, 8, .2);
        VerdigrisFx.sound(level, from, net.minecraft.sounds.SoundEvents.AXE_SCRAPE, .8f, 1.2f);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
