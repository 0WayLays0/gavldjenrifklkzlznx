package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
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
 * Base of the Stellar Fire spells: the school, the config, the caster's gesture, and the heat. Every spell comes in
 * the six heats, each its own spell ({@code <name>_<heat>}: red, orange, yellow, white, blue, violet): it burns in
 * that colour and strikes the harder the hotter it is ({@link HeatColors#POWER}), for a little more mana.
 */
public abstract class StarfireSpell extends AbstractSpell {
    private final ResourceLocation spellId;
    /** The heat it burns at (0 red to 5 violet). */
    protected final int tier;
    private final DefaultConfig defaultConfig;
    private final CastType castType;
    @Nullable
    private final AnimationHolder animation;

    /** {@code animation} null: Iron's own for the cast type. */
    protected StarfireSpell(String name, int tier, SpellRarity minRarity, int maxLevel, int cooldownSeconds, CastType castType,
                            @Nullable AnimationHolder animation) {
        this.tier = HeatColors.clamp(tier);
        this.spellId = ArcanesOublies.id(name + "_" + HeatColors.ID[this.tier]);
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

    /** The heat it burns at (0 red to 5 violet). */
    public int tier() {
        return tier;
    }

    /** The heat it burns at: the same whoever casts it. */
    public int heat(@Nullable LivingEntity caster) {
        return tier;
    }

    /** The spell's power at its heat. */
    public float hot(int spellLevel, @Nullable LivingEntity caster) {
        return getPower(spellLevel, caster) * HeatColors.POWER[tier];
    }

    /** The hotter, the dearer: subclasses call it once their mana costs are set. */
    protected void costsByHeat() {
        float k = 1 + .12f * tier;
        this.baseManaCost = Math.round(this.baseManaCost * k);
        this.manaCostPerLevel = Math.round(this.manaCostPerLevel * k);
    }

    /** The tooltip line giving its heat (its star class and temperature) and how much harder it makes the spell strike. */
    public MutableComponent heatLine(@Nullable LivingEntity caster) {
        return Component.translatable("ui.arcanes_oublies.heat_of", Component.translatable("heat.arcanes_oublies." + tier), HeatColors.CLASS[tier],
                HeatColors.KELVIN[tier], Utils.stringTruncation(HeatColors.POWER[tier], 2)).withStyle(s -> s.withColor(HeatColors.BODY[tier]));
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
