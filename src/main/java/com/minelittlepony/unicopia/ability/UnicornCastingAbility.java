package com.minelittlepony.unicopia.ability;

import java.util.Random;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Streams;
import com.minelittlepony.unicopia.Race;
import com.minelittlepony.unicopia.ability.data.Hit;
import com.minelittlepony.unicopia.ability.magic.spell.effect.CustomisedSpellType;
import com.minelittlepony.unicopia.ability.magic.spell.effect.SpellType;
import com.minelittlepony.unicopia.entity.player.Pony;
import com.minelittlepony.unicopia.item.AmuletItem;
import com.minelittlepony.unicopia.item.GemstoneItem;
import com.minelittlepony.unicopia.particle.MagicParticleEffect;

import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;

/**
 * Casts magic onto the user directly, or uses the item in the main hand slot.
 * <p>
 * 1. If the player is holding nothing, defaults to toggling their equipped spell (currently only shield).
 * 2. If the player is holding a gem, consumes it and casts whatever spell is contained within onto the user.
 * 3. If the player is holding a amulet, charges it.
 */
public class UnicornCastingAbility implements Ability<Hit> {

    @Override
    public int getWarmupTime(Pony player) {
        return 20;
    }

    @Override
    public int getCooldownTime(Pony player) {
        return 0;
    }

    @Override
    public boolean canUse(Race race) {
        return race.canCast();
    }

    @Override
    @Nullable
    public Hit tryActivate(Pony player) {
        float manaLevel = player.getMagicalReserves().getMana().get();

        TypedActionResult<ItemStack> amulet = getAmulet(player);

        if (amulet.getResult().isAccepted()) {
            return Hit.of(manaLevel > 0 && ((AmuletItem)amulet.getValue().getItem()).canCharge(amulet.getValue()));
        }

        ActionResult spell = getNewSpell(player).getResult();

        if (spell != ActionResult.PASS) {
            return Hit.of(spell != ActionResult.FAIL && manaLevel > 4F);
        }

        return Hit.of(manaLevel > (player.getSpellSlot().isPresent() ? 2F : 4F));
    }

    @Override
    public Hit.Serializer<Hit> getSerializer() {
        return Hit.SERIALIZER;
    }

    @Override
    public double getCostEstimate(Pony player) {
        TypedActionResult<ItemStack> amulet = getAmulet(player);

        if (amulet.getResult().isAccepted()) {
            float manaLevel = player.getMagicalReserves().getMana().get();

            return Math.min(manaLevel, ((AmuletItem)amulet.getValue().getItem()).getChargeRemainder(amulet.getValue()));
        }

        if (getNewSpell(player).getResult() == ActionResult.CONSUME) {
            return 4F;
        }

        if (player.getSpellSlot().isPresent()) {
            return 2F;
        }

        return 4F;
    }

    @Override
    public void apply(Pony player, Hit data) {
        TypedActionResult<ItemStack> amulet = getAmulet(player);

        if (amulet.getResult().isAccepted()) {
            ItemStack stack = amulet.getValue();
            AmuletItem item = (AmuletItem)stack.getItem();

            if (item.canCharge(stack)) {
                float amount = -Math.min(player.getMagicalReserves().getMana().get(), item.getChargeRemainder(stack));

                if (amount < 0) {
                    AmuletItem.consumeEnergy(stack, amount);
                    player.getMagicalReserves().getMana().add(amount * player.getMagicalReserves().getMana().getMax());
                    player.getWorld().playSoundFromEntity(null, player.getMaster(), SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 1, 1);
                }
            }
        } else {
            TypedActionResult<CustomisedSpellType<?>> newSpell = getNewSpell(player);

            if (newSpell.getResult() != ActionResult.FAIL) {
                CustomisedSpellType<?> spell = newSpell.getValue();

                boolean remove = player.getSpellSlot().removeIf(spell, true);
                player.subtractEnergyCost(remove ? 2 : 4);
                if (!remove) {
                    spell.apply(player);
                }
            }
        }
    }

    private TypedActionResult<ItemStack> getAmulet(Pony player) {

        ItemStack stack = player.getMaster().getStackInHand(Hand.MAIN_HAND);

        if (stack.getItem() instanceof AmuletItem) {
            if (((AmuletItem)stack.getItem()).isChargable()) {
                return TypedActionResult.consume(stack);
            }

            return TypedActionResult.success(stack);
        }

        return TypedActionResult.pass(stack);
    }

    private TypedActionResult<CustomisedSpellType<?>> getNewSpell(Pony player) {
        return Streams.stream(player.getMaster().getItemsHand())
                .filter(GemstoneItem::isEnchanted)
                .map(stack -> GemstoneItem.consumeSpell(stack, player.getMaster(), null, null))
                .findFirst()
                .orElse(TypedActionResult.<CustomisedSpellType<?>>pass(SpellType.SHIELD.withTraits()));
    }

    @Override
    public void preApply(Pony player, AbilitySlot slot) {
        player.getMagicalReserves().getExhaustion().multiply(3.3F);

        if (getAmulet(player).getResult() == ActionResult.CONSUME) {
            Vec3d eyes = player.getMaster().getCameraPosVec(1);

            float i = player.getAbilities().getStat(slot).getFillProgress();

            Random rng = player.getWorld().random;

            player.getWorld().addParticle(i > 0.5F ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD, eyes.x, eyes.y, eyes.z,
                    (rng.nextGaussian() - 0.5) / 10,
                    (rng.nextGaussian() - 0.5) / 10,
                    (rng.nextGaussian() - 0.5) / 10
            );
            player.getWorld().playSound(player.getEntity().getX(), player.getEntity().getY(), player.getEntity().getZ(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 1, i / 20, true);
        } else {
            player.spawnParticles(MagicParticleEffect.UNICORN, 5);
        }
    }

    @Override
    public void postApply(Pony player, AbilitySlot slot) {
        player.spawnParticles(MagicParticleEffect.UNICORN, 5);
    }
}
