package com.waylays.arcanes.spells.bone;

import com.waylays.arcanes.entity.bone.BoneLance;
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

/** Bone Volley: a spray of bone splinters fanning out from the caster's hand. */
@AutoSpellConfig
public class BoneVolleySpell extends BoneSpell {
    public static final float CONE = .5f;

    public BoneVolleySpell() {
        super("bone_volley", SpellRarity.RARE, 6, 10, CastType.INSTANT, SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION);
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    public static int shards(int spellLevel) {
        return 8 + 2 * spellLevel;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.arcanes_oublies.shards", shards(spellLevel), Utils.stringTruncation(getPower(spellLevel, caster), 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        Vec3 look = entity.getLookAngle();
        Vec3 from = entity.getEyePosition().add(look.scale(.7)).subtract(0, .25, 0);
        int count = shards(spellLevel);
        for (int i = 0; i < count; i++) {
            // Spread over the cone, evenly across and a little up and down
            float across = (i / (float) (count - 1) - .5f) * 2 * CONE + (level.random.nextFloat() - .5f) * .12f;
            float up = (level.random.nextFloat() - .5f) * CONE * .5f;
            Vec3 dir = look.yRot(across).xRot(up);
            BoneLance.throwBone(level, entity, this, BoneLance.Kind.SHARD, from, dir.scale(1.6 + level.random.nextFloat() * .4), getPower(spellLevel, entity));
        }
        BoneFx.crack(level, from, 1);
        BoneFx.splinters(level, from, 8, .2);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
