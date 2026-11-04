package com.waylays.arcanes.spells.clockwork;

import com.waylays.arcanes.entity.clockwork.ClockworkScene;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/** Rivet Volley: a fan of red-hot rivets, each striking the first enemy in its way. */
@AutoSpellConfig
public class RivetVolleySpell extends ClockworkSpell {
    public RivetVolleySpell() {
        super("rivet_volley", SpellRarity.COMMON, 10, 3, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 2;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    public static int rivets(int spellLevel) {
        return 3 + (spellLevel - 1) / 3;
    }

    public float rivet(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * .5f;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.arcanes_oublies.rivets", rivets(spellLevel), Utils.stringTruncation(rivet(spellLevel, caster), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            int n = rivets(spellLevel);
            float yaw = entity.getYRot(), pitch = entity.getXRot();
            for (int k = 0; k < n; k++) {
                float spread = (k - (n - 1) * .5f) * 7f;
                Vec3 d = Vec3.directionFromRotation(pitch, yaw + spread);
                ClockworkScene.spawn(server, entity, this, ClockworkScene.Kind.RIVET, entity.getEyePosition().add(0, -.3, 0).add(d.scale(.8)), d.toVector3f(), .3f,
                        ClockworkScene.RIVET_FLIGHT + ClockworkScene.BURST + 2, rivet(spellLevel, entity));
            }
            ClockworkFx.steam(level, entity.getEyePosition().add(entity.getLookAngle()), 6, .2);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.2f, 1.4f);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, .8f, 1.8f);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
