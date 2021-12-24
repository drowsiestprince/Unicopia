package com.minelittlepony.unicopia.network.datasync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class NetworkedReferenceSet<T> {

    private final List<UUID> ids = new ArrayList<>();

    private final Map<UUID, NetworkedReference<T>> values = new HashMap<>();

    private final Function<T, UUID> uuidConverter;
    private final Supplier<NetworkedReference<T>> factory;

    private boolean dirty;
    private boolean reading;

    public NetworkedReferenceSet(Function<T, UUID> uuidConverter, Supplier<NetworkedReference<T>> factory) {
        this.uuidConverter = uuidConverter;
        this.factory = factory;
    }

    public boolean containsReference(UUID id) {
        return ids.contains(id);
    }

    public Stream<T> getReferences() {
        return ids.stream().map(id -> values.get(id))
                .map(a -> a.getReference())
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public boolean clear() {
        dirty |= !ids.isEmpty() || !values.isEmpty();
        ids.clear();
        values.clear();
        return dirty;
    }

    public void addReference(@Nullable T newValue) {
        if (newValue != null) {
            addReference(uuidConverter.apply(newValue)).updateReference(newValue);
        }
    }

    private NetworkedReference<T> addReference(UUID newValue) {
        return values.computeIfAbsent(newValue, id -> {
            dirty = true;
            ids.remove(id);
            ids.add(0, id);
            return factory.get();
        });
    }

    public void removeReference(@Nullable T oldValue) {
        if (oldValue != null) {
            removeReference(uuidConverter.apply(oldValue));
        }
    }

    private void removeReference(UUID id) {
        dirty |= ids.remove(id);
        NetworkedReference<T> i = values.remove(id);
        if (i != null) {
            dirty = true;
            i.updateReference(null);
        }
    }

    public boolean fromNbt(NbtCompound comp) {
        if (reading) {
            return false;
        }
        reading = true;
        try {
            List<UUID> incoming = new ArrayList<>();
            comp.getList("keys", NbtElement.STRING_TYPE).forEach(key -> {
                incoming.add(UUID.fromString(key.asString()));
            });

            ids.stream().filter(id -> !incoming.contains(id)).toList().forEach(this::removeReference);

            boolean[] send = new boolean[1];
            incoming.forEach(key -> {
                NetworkedReference<T> i = addReference(key);
                send[0] |= i.fromNbt(comp.getCompound(key.toString()));
                if (i.getReference().isEmpty()) {
                    removeReference(key);
                }
            });
            dirty = send[0];
            return send[0];
        } finally {
            reading = false;
        }
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        NbtList ids = new NbtList();
        this.ids.forEach(id -> {
            String sid = id.toString();
            ids.add(NbtString.of(sid));
            tag.put(sid, values.get(id).toNbt());
        });
        tag.put("keys", ids);
        dirty = false;
        return tag;
    }

    public boolean isDirty() {
        return dirty || values.values().stream().anyMatch(NetworkedReference::isDirty);
    }
}