package com.mattmx.nametags.packet;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TAB (and any other scoreboard-team nametag hider) sets
 * {@code nametagVisibility=never} on the player's team. Since Minecraft 1.9
 * the client copies that team onto tamed animals via the owner UUID in entity
 * metadata, so named wolves/cats/horses vanish even though their
 * {@code CustomName} is fine.
 * <p>
 * Clearing the owner UUID on the outgoing packet stops the client applying
 * the owner's team. Sitting, collars, and server-side ownership are
 * unaffected; the tamed flag is a separate metadata byte.
 */
public final class TameableOwnerHider {

    private TameableOwnerHider() {
    }

    public static boolean isPresentOwnerUuid(@Nullable Object value) {
        return value instanceof Optional<?> optional
                && optional.isPresent()
                && optional.get() instanceof UUID;
    }

    /**
     * @return true if any present owner UUID was cleared
     */
    @SuppressWarnings("unchecked")
    public static boolean clearPresentOwner(@NotNull List<? extends EntityData<?>> metadata) {
        boolean changed = false;
        for (EntityData<?> entry : metadata) {
            if (!isPresentOwnerUuid(entry.getValue())) {
                continue;
            }
            ((EntityData<Optional<UUID>>) entry).setValue(Optional.empty());
            changed = true;
        }
        return changed;
    }
}
