package com.mattmx.nametags.visibility;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PermissiveNameTagVisibilityProvider implements NameTagVisibilityProvider {
    public static final PermissiveNameTagVisibilityProvider INSTANCE = new PermissiveNameTagVisibilityProvider();

    private PermissiveNameTagVisibilityProvider() {
    }

    @Override
    public boolean canSee(@NotNull Player viewer, @NotNull Player target) {
        return true;
    }
}
