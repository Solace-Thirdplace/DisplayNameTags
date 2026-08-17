package com.mattmx.nametags.entity;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * When to show a packet item-display head instead of parenting the nametag to
 * the real player. Survival/Creative viewers cannot see a spectator entity (or
 * its passengers); other spectators already see the vanilla floating head.
 */
public final class SpectatorStandinPolicy {

    private SpectatorStandinPolicy() {
    }

    public static boolean showStandinTo(
            boolean featureEnabled,
            boolean ownerSpectator,
            boolean viewerSpectator,
            boolean viewerIsOwner) {
        return featureEnabled && ownerSpectator && !viewerSpectator && !viewerIsOwner;
    }

    public static boolean showStandinTo(boolean featureEnabled, @Nullable Player owner, @NotNull Player viewer) {
        if (owner == null) {
            return false;
        }
        return showStandinTo(
                featureEnabled,
                owner.getGameMode() == GameMode.SPECTATOR,
                viewer.getGameMode() == GameMode.SPECTATOR,
                viewer.equals(owner));
    }
}
