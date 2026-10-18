package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.entity.SpellProjectile;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * A razor-edged page thrown like a blade: it spins flat through the air, slices through up to {@code pierce} extra
 * enemies and shreds itself on the first wall. Also thrown back at attackers by Parchment Aegis (the spell it counts
 * for is kept by id).
 */
public class RazorPage extends SpellProjectile {
    public static final int LIFETIME = 30;

    private int pierce;
    private String spellId = ModSpells.RAZOR_PAGES.getId().toString();
    private final Set<Integer> hit = new HashSet<>();

    public RazorPage(EntityType<? extends RazorPage> entityType, Level level) {
        super(entityType, level);
    }

    public RazorPage(Level level, LivingEntity owner, float damage, int pierce, AbstractSpell spell) {
        this(ModEntities.RAZOR_PAGE.get(), level);
        setOwner(owner);
        setDamage(damage);
        this.pierce = pierce;
        this.spellId = spell.getSpellId();
    }

    @Override
    protected int lifetime() {
        return LIFETIME;
    }

    @Override
    protected boolean piercesEntities() {
        return true;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !hit.contains(target.getId()) && !SpellUtil.isAlly(getOwner(), target);
    }

    @Override
    protected void clientTick() {
        Vec3 p = position();
        level().addParticle(ParticleTypes.ENCHANT, p.x, p.y, p.z, 0, 0, 0);
        if (tickCount % 2 == 0) {
            level().addParticle(BookFx.GILT_DUST, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    @Override
    protected void onImpact(HitResult result) {
        if (result instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            hit.add(target.getId());
            if (target instanceof LivingEntity living) {
                // A volley of pages is many cuts, each one through hurt immunity
                living.invulnerableTime = 0;
                AbstractSpell spell = SpellRegistry.getSpell(spellId);
                DamageSources.applyDamage(living, damage, SpellUtil.damageSource(spell, this, getOwner()));
            }
            Vec3 c = target.getBoundingBox().getCenter();
            SpellUtil.particles(level(), ParticleTypes.CRIT, c.x, c.y, c.z, 6, .2, .2, .2, .3);
            SpellUtil.particles(level(), BookFx.paper(), c.x, c.y, c.z, 2, .1, .1, .1, .1);
            level().playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .6f, 1.6f + random.nextFloat() * .3f);
            if (--pierce < 0) {
                shred(c);
            }
        } else {
            shred(result.getLocation());
        }
    }

    @Override
    protected void onExpire() {
        shred(position());
    }

    /** The page tears apart into scraps and glyphs. */
    private void shred(Vec3 at) {
        SpellUtil.particles(level(), BookFx.paper(), at.x, at.y, at.z, 6, .15, .15, .15, .12);
        SpellUtil.particles(level(), ParticleTypes.ENCHANT, at.x, at.y, at.z, 10, .1, .1, .1, .6);
        level().playSound(null, at.x, at.y, at.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, .8f, 1.4f + random.nextFloat() * .3f);
        discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Pierce", pierce);
        tag.putString("Spell", spellId);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pierce = tag.getInt("Pierce");
        spellId = tag.getString("Spell");
    }
}
