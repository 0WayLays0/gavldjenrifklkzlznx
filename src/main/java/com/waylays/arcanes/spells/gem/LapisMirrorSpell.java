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
 * Lapis Mirror: a tablet of lapis in a frame of gold circles the caster for ten seconds: enemy shots that come near the
 * caster are turned back, and every two seconds it binds the nearest enemy in sight with a chain of golden light:
 * struck, silenced and slowed.
 */
@AutoSpellConfig
public class LapisMirrorSpell extends GemSpell {
    public static final int LIFE = 212;

    public LapisMirrorSpell() {
        super("lapis_mirror", SpellRarity.UNCOMMON, 8, 25, CastType.INSTANT, SpellAnimations.SELF_CAST_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 45;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.mirror_turns"),
                Component.translatable("ui.arcanes_oublies.damage_every", Utils.stringTruncation(getPower(spellLevel, caster), 1),
                        Utils.timeFromTicks(GemScene.MIRROR_EVERY, 1)),
                Component.translatable("ui.arcanes_oublies.mirror_binds", Utils.timeFromTicks(40, 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(LIFE - 12, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            GemScene.spawn(server, entity, this, GemScene.Kind.MIRROR, entity.position(), new Vector3f(0, 0, 1), 1, LIFE, getPower(spellLevel, entity))
                    .follow(entity);
            GemFx.sigil(level, SpellUtil.groundAt(level, entity.position(), 2, 1), Gem.LAPIS, 2, 20);
            GemFx.chime(level, entity.position(), 1.4f, 1f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
