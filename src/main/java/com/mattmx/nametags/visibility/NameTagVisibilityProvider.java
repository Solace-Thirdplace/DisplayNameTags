package com.mattmx.nametags.visibility;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface NameTagVisibilityProvider {
    boolean canSee(@NotNull Player viewer, @NotNull Player target);
}
