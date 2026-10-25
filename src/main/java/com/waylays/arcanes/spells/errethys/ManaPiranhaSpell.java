package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.registry.ModEntities;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Mana Piranha: a piranha of mana that lives only in water and bites in a frenzy. It takes shape in the water the
 * caster looks at, else in the nearest water around them; with no water nearby the spell fails.
 */
@AutoSpellConfig
public class ManaPiranhaSpell extends ManaSummonSpell {
    /** How far the caster can look for the water. */
    public static final double LOOK_RANGE = 12;
    /** How far around the caster the nearest water is searched for. */
    public static final int SEARCH_RADIUS = 6;

    public ManaPiranhaSpell() {
        super("mana_piranha", SpellRarity.UNCOMMON, 6, 25);
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 2;
        this.castTime = 20;
        this.baseManaCost = 40;
    }

    @Override
    protected EntityType<? extends ManaSummon> summonType() {
        return ModEntities.MANA_PIRANHA.get();
    }

    /** No water, no piranha (sending one away always works). */
    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (hasOpenRecast(entity) || findWater(level, entity) != null) {
            return true;
        }
        if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.arcanes_oublies.needs_water").withStyle(ChatFormatting.RED)));
        }
        return false;
    }

    @Override
    @Nullable
    protected Vec3 summonSpot(Level level, LivingEntity entity) {
        return findWater(level, entity);
    }

    /** The water block the caster looks at, else the nearest one around them, as the middle of the block. */
    @Nullable
    public static Vec3 findWater(Level level, LivingEntity caster) {
        Vec3 eye = caster.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(caster.getLookAngle().scale(LOOK_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, caster));
        if (hit.getType() == HitResult.Type.BLOCK && isSwimmable(level, hit.getBlockPos())) {
            return Vec3.atCenterOf(hit.getBlockPos());
        }
        BlockPos origin = caster.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-SEARCH_RADIUS, -4, -SEARCH_RADIUS), origin.offset(SEARCH_RADIUS, 2, SEARCH_RADIUS))) {
            double distance = pos.distSqr(origin);
            if (distance < bestDistance && isSwimmable(level, pos)) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best == null ? null : Vec3.atCenterOf(best);
    }

    /** Water a fish fits in: not a waterlogged stair or slab. */
    private static boolean isSwimmable(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /** 30 s at level 1, +5 s per level. */
    @Override
    public int getLifetime(int spellLevel) {
        return 500 + spellLevel * 100;
    }

    @Override
    public double getHealth(int spellLevel) {
        return 25;
    }
}
