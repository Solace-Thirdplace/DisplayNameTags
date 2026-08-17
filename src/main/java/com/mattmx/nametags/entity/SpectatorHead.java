package com.mattmx.nametags.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import com.github.retrooper.packetevents.util.Vector3f;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.ItemDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Packet-only player-head item display that follows a spectator so
 * Survival/Creative viewers have something to look at (and to parent the
 * nametag to). Not shown to other spectators or to the owner.
 */
public final class SpectatorHead {

    private final @NotNull WrapperEntity display;

    public SpectatorHead(@NotNull Player owner, int teleportDuration) {
        this.display = new WrapperEntity(EntityTypes.ITEM_DISPLAY);
        this.display.spawn(locationOf(owner));
        this.display.consumeEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setItem(headOf(owner));
            meta.setDisplayType(ItemDisplayMeta.DisplayType.HEAD);
            meta.setBillboardConstraints(AbstractDisplayMeta.BillboardConstraints.FIXED);
            meta.setPositionRotationInterpolationDuration(Math.max(0, teleportDuration));
            meta.setScale(new Vector3f(1f, 1f, 1f));
            meta.setViewRange(50f);
        });
    }

    public int getEntityId() {
        return display.getEntityId();
    }

    public @NotNull WrapperEntity getEntity() {
        return display;
    }

    public void updateLocation(@NotNull Player owner) {
        display.setLocation(locationOf(owner));
    }

    public void addViewer(@NotNull UUID viewer) {
        display.addViewer(viewer);
    }

    public void removeViewer(@NotNull UUID viewer) {
        display.removeViewer(viewer);
    }

    public boolean hasViewer(@NotNull UUID viewer) {
        return display.getViewers().contains(viewer);
    }

    public void destroy() {
        display.despawn();
    }

    private static @NotNull Location locationOf(@NotNull Player owner) {
        org.bukkit.Location eye = owner.getEyeLocation();
        Location location = SpigotConversionUtil.fromBukkitLocation(eye);
        location.setYaw(owner.getLocation().getYaw());
        location.setPitch(owner.getLocation().getPitch());
        return location;
    }

    private static @NotNull ItemStack headOf(@NotNull Player owner) {
        ItemProfile profile = new ItemProfile(owner.getName(), owner.getUniqueId(), List.of());
        return ItemStack.builder()
                .type(ItemTypes.PLAYER_HEAD)
                .amount(1)
                .component(ComponentTypes.PROFILE, profile)
                .build();
    }
}
