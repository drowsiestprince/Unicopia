package com.minelittlepony.unicopia.network;

import com.minelittlepony.unicopia.InteractionManager;
import com.minelittlepony.unicopia.util.network.Packet;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Sent by the server to update block destruction progress on the client.
 */
public class MsgBlockDestruction implements Packet<PlayerEntity> {

    private final Long2ObjectMap<Integer> destructions;

    MsgBlockDestruction(PacketByteBuf buffer) {
        destructions = new Long2ObjectOpenHashMap<>();
        int size = buffer.readInt();
        for (int i = 0; i < size; i++) {
            destructions.put(buffer.readLong(), (Integer)buffer.readInt());
        }
    }

    public MsgBlockDestruction(Long2ObjectMap<Integer> destructions) {
        this.destructions = destructions;
    }

    public Long2ObjectMap<Integer> getDestructions() {
        return destructions;
    }

    @Override
    public void toBuffer(PacketByteBuf buffer) {
        buffer.writeInt(destructions.size());
        destructions.forEach((p, i) -> {
            buffer.writeLong(p);
            buffer.writeInt(i);
        });
    }

    @Override
    public void handle(PlayerEntity sender) {
        InteractionManager.instance().getClientNetworkHandler().handleBlockDestruction(this);
    }
}
