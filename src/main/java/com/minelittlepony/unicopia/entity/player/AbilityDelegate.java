package com.minelittlepony.unicopia.entity.player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.minelittlepony.unicopia.Unicopia;
import com.minelittlepony.unicopia.ability.IAbilityReceiver;
import com.minelittlepony.unicopia.ability.IData;
import com.minelittlepony.unicopia.ability.IPower;
import com.minelittlepony.unicopia.ability.powers.PowersRegistry;
import com.minelittlepony.unicopia.entity.IUpdatable;
import com.minelittlepony.unicopia.network.MsgPlayerAbility;
import com.minelittlepony.util.InbtSerialisable;

import net.minecraft.nbt.CompoundTag;

class AbilityDelegate implements IAbilityReceiver, IUpdatable, InbtSerialisable {

    private final IPlayer player;

    /**
     * Ticks of warmup before an ability is triggered.
     */
    private int warmup;

    /**
     * Ticks of cooldown after an ability has been triggered.
     */
    private int cooldown;

    /**
     * True once the current ability has been triggered.
     */
    private boolean triggered;

    @Nullable
    private IPower<?> activeAbility = null;

    public AbilityDelegate(IPlayer player) {
        this.player = player;
    }

    /**
     * Returns true if the currrent ability can we swapped out.
     */
    boolean canSwitchStates() {
        return activeAbility == null || (warmup != 0) || (triggered && cooldown == 0);
    }

    @Override
    public void tryUseAbility(IPower<?> power) {
        if (canSwitchStates()) {
            setAbility(power);
        }
    }

    @Override
    public void tryClearAbility() {
        if (canSwitchStates()) {
            setAbility(null);
        }
    }

    protected synchronized void setAbility(@Nullable IPower<?> power) {
        if (activeAbility != power) {
            triggered = false;
            activeAbility = power;
            warmup = power == null ? 0 : power.getWarmupTime(player);
            cooldown = 0;
        }
    }

    @Nullable
    protected synchronized IPower<?> getUsableAbility() {
        if (!(activeAbility == null || (triggered && warmup == 0 && cooldown == 0)) && activeAbility.canUse(player.getSpecies())) {
            return activeAbility;
        }
        return null;
    }

    @Override
    public int getRemainingCooldown() {
        return cooldown;
    }

    @Override
    public void onUpdate() {
        IPower<?> ability = getUsableAbility();

        if (ability == null) {
            return;
        }

        if (warmup > 0) {
            warmup--;
            ability.preApply(player);
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            ability.postApply(player);
            return;
        }

        if (triggered) {
            return;
        }

        if (ability.canActivate(player.getWorld(), player)) {
            triggered = true;
            cooldown = ability.getCooldownTime(player);

            if (player.isClientPlayer()) {
                if (!activateAbility(ability)) {
                    cooldown = 0;
                }
            }
        }

        if (cooldown <= 0) {
            setAbility(null);
        }
    }

    @Override
    public void toNBT(CompoundTag compound) {
        compound.putBoolean("triggered", triggered);
        compound.putInt("warmup", warmup);
        compound.putInt("cooldown", cooldown);

        IPower<?> ability = getUsableAbility();

        if (ability != null) {
            compound.putString("activeAbility", ability.getKeyName());
        }
    }

    @Override
    public void fromNBT(CompoundTag compound) {
        activeAbility = null;

        triggered = compound.getBoolean("triggered");
        warmup = compound.getInt("warmup");
        cooldown = compound.getInt("cooldown");

        if (compound.containsKey("activeAbility")) {
            PowersRegistry.instance()
                .getPowerFromName(compound.getString("activeAbility"))
                .ifPresent(p -> activeAbility = p);
        }
    }

    /**
     * Attempts to activate the current stored ability.
     * Returns true if the ability suceeded, otherwise false.
     */
    protected boolean activateAbility(@Nonnull IPower<?> ability) {
        IData data = ability.tryActivate(player);

        if (data != null) {
            Unicopia.getConnection().send(new MsgPlayerAbility(player.getOwner(), ability, data), Target.SERVER);
        }

        return data != null;
    }
}