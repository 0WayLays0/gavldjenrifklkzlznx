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
 * Shooting Star: a star of lapis flecked and rimmed with gold flies from the caster's hand, turning toward the enemy
 * they look at, and bursts on it in golden stars: it strikes everything near and weighs it down.
 */
@AutoSpellConfig
public class ShootingStarSpell extends GemSpell {
    public static final float BLAST = 2;

    public ShootingStarSpell() {
        super("shooting_star", SpellRarity.COMMON, 10, 5, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.castTime = 0;
        this.baseManaCost = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getPower(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(BLAST, 1)),
                Component.translatable("ui.arcanes_oublies.hamstring"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            Vec3 look = entity.getLookAngle();
            Vec3 from = entity.getEyePosition().add(0, -.2, 0).add(look.scale(.8));
            GemScene.spawn(server, entity, this, GemScene.Kind.STAR, from, look.toVector3f(), BLAST, GemScene.STAR_FLIGHT + GemScene.STAR_BURST + 2,
                    getPower(spellLevel, entity)).follow(SpellUtil.aimedEnemy(level, entity, 30));
            GemFx.sigil(level, SpellUtil.groundAt(level, entity.position(), 2, 1), Gem.LAPIS, 1.6f, 14);
            GemFx.chime(level, entity.position(), 1.2f, 1.7f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
