package com.waylays.arcanes.spells.cosmos;

import com.waylays.arcanes.entity.cosmos.TimeDoor;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pocket Universe, out of time: a TVA door opens in front of the caster and takes them into their own universe in a
 * pocket, a floating island in deep space where time stands still, to keep their things and build what they like;
 * no one else can touch anything there. The island grows with the level the spell is cast at. Friends crouching
 * next to the caster are taken along. Cast inside, or walk into the door on the island, to go back.
 */
@AutoSpellConfig
public class PocketUniverseSpell extends CosmosSpell {
    public PocketUniverseSpell() {
        super("pocket_universe", SpellRarity.RARE, PocketUniverse.MAX_LEVEL, 10, CastType.LONG, null);
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 30;
        this.baseManaCost = 60;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.arcanes_oublies.pocket_island", PocketUniverse.radius(spellLevel) * 2),
                Component.translatable("ui.arcanes_oublies.pocket_guests"));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (entity instanceof ServerPlayer player && PocketUniverse.isIn(player)) {
            PocketUniverse.leave(player);
        } else if (!level.isClientSide) {
            List<UUID> guests = new ArrayList<>();
            for (ServerPlayer other : level.getEntitiesOfClass(ServerPlayer.class, entity.getBoundingBox().inflate(3),
                    p -> p != entity && p.isAlive() && p.isShiftKeyDown())) {
                guests.add(other.getUUID());
            }
            Vec3 forward = Vec3.directionFromRotation(0, entity.getYRot());
            Vec3 at = SpellUtil.groundAt(level, entity.position().add(forward.scale(2.4)), 3, 3);
            TimeDoor.entry(level, entity, at, spellLevel, guests);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
