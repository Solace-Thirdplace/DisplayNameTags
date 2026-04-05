package com.mattmx.nametags.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.mattmx.nametags.NameTags;
import com.mattmx.nametags.entity.NameTagEntity;
import com.mattmx.nametags.hook.VanishHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Responsible for appending the name tag spawn packet and
 * passenger packet with the name tag entity when sending
 * a [WrapperPlayServerSpawnEntity]
 * packet to the client.
 */
public class PlayServerSpawnEntityHandler {

    public static void handlePacket(@NotNull PacketSendEvent event) {
        final NameTags plugin = NameTags.getInstance();
        final WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(event);

        if (packet.getUUID().isEmpty())
            return;

        final UUID packetUUID = packet.getUUID().get();
        final NameTagEntity nameTagEntity = plugin.getEntityManager().getNameTagEntityByUUID(packetUUID);

        final User user = event.getUser();
        if (nameTagEntity == null) {

            // Entity doesn't have a nametag yet. For players this can happen when the
            // SPAWN_ENTITY packet races the PlayerJoinEvent, or after a Caffeine cache
            // eviction window. Schedule a main-thread retry that will create the tag
            // if it still doesn't exist.
            if (packet.getEntityType() == EntityTypes.PLAYER) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    final Player player = Bukkit.getPlayer(packetUUID);
                    if (player == null || !player.isOnline())
                        return;

                    final NameTagEntity resolved = plugin.getEntityManager()
                            .getOrCreateNameTagEntity(player);
                    resolved.updateVisibility();

                    plugin.getExecutor().execute(() -> attachPassengerToEntity(resolved, user));
                }, 20L);
            }

            return;
        }

        // Add passenger and send to player after (off the netty thread)
        event.getTasksAfterSend()
                .add(() -> plugin.getExecutor().execute(() -> attachPassengerToEntity(nameTagEntity, user)));
    }

    private static void attachPassengerToEntity(final NameTagEntity nameTagEntity, final User receiver) {
        final NameTags plugin = NameTags.getInstance();

        // Check if the nametag is disabled by an admin
        if (plugin.getEntityManager().isNameTagDisabled(nameTagEntity.getBukkitEntity().getUniqueId())) {
            return;
        }

        // Check if the nametag owner is vanished from the receiver
        if (nameTagEntity.getBukkitEntity() instanceof Player target) {
            Player viewer = Bukkit.getPlayer(receiver.getUUID());
            if (viewer != null && !VanishHook.canSee(viewer, target)) {
                // Viewer can't see the vanished player, don't show nametag
                return;
            }
        }

        // Check if the entity is invisible (e.g., from invisibility potion)
        // Don't show nametag if they're invisible (vanilla behavior)
        // unless the viewer has debug view enabled
        if (nameTagEntity.isInvisible() && !plugin.getEntityManager().hasDebugView(receiver.getUUID())) {
            return;
        }

        // To avoid name tag moving when being added
        nameTagEntity.updateLocation();

        // Refreshes as viewer (crusty fix)
        nameTagEntity.getPassenger().removeViewer(receiver);
        nameTagEntity.getPassenger().addViewer(receiver);

        receiver.sendPacket(nameTagEntity.getPassengersPacket());
    }

}
