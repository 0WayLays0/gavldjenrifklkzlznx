package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.resources.ResourceLocation;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Base of the Stellar Fire spells: the school, the config, the caster's gesture, and the heat: each is cast at the
 * caster's heat (its colour, {@link #hot} its strength) and then stokes it ({@link #stokes} steps).
 */
public abstract class StarfireSpell extends AbstractSpell {
    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;
    private final CastType castType;
    @Nullable
    private final AnimationHolder animation;

    /** {@code animation} null: Iron's own for the cast type. */
    protected StarfireSpell(String name, SpellRarity minRarity, int maxLevel, int cooldownSeconds, CastType castType, @Nullable AnimationHolder animation) {
        this.spellId = ArcanesOublies.id(name);
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(minRarity)
                .setSchoolResource(ModSchools.STARFIRE_RESOURCE)
                .setMaxLevel(maxLevel)
                .setCooldownSeconds(cooldownSeconds)
                .build();
        this.castType = castType;
        this.animation = animation;
    }

    @Override
    public CastType getCastType() {
        return castType;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return animation != null ? animation : super.getCastStartAnimation();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return animation != null ? AnimationHolder.pass() : super.getCastFinishAnimation();
    }

    public float getPower(int spellLevel, @Nullable LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }

    /** The caster's heat now (0 red to 5 violet). */
    public static int heat(@Nullable LivingEntity caster) {
        return caster == null ? 0 : Heat.of(caster);
    }

    /** The spell's power at the caster's heat now. */
    public float hot(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * HeatColors.POWER[heat(caster)];
    }

    /** How many steps casting it stokes the caster's heat. */
    protected int stokes() {
        return 1;
    }

    /** The tooltip line giving the caster's heat now and how much harder it makes the spell strike. */
    public static MutableComponent heatLine(@Nullable LivingEntity caster) {
        int tier = heat(caster);
        return Component.translatable("ui.arcanes_oublies.heat_now", Component.translatable("heat.arcanes_oublies." + tier),
                Utils.stringTruncation(HeatColors.POWER[tier], 2)).withStyle(s -> s.withColor(HeatColors.BODY[tier]));
    }

    /** Subclasses call this last, once their flames are out at the old heat: it stokes it. */
    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && stokes() > 0) {
            Heat.stoke(entity, stokes());
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** The enemy the caster looks at within {@code range}, if any. */
    @Nullable
    public static LivingEntity aimed(Level level, LivingEntity caster, double range) {
        return SpellUtil.aimedEnemy(level, caster, range);
    }

    /** Where the caster's aim meets the ground: under the enemy they look at, else the ground they point at. */
    public static Vec3 aimedSpot(Level level, LivingEntity caster, double range) {
        return SpellUtil.aimedSpot(level, caster, range);
    }
}
