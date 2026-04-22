package com.mattmx.nametags.visibility;

import com.mattmx.nametags.NameTags;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class ExyliaEventsVisibilityProvider implements NameTagVisibilityProvider {
    private static final String EXYLIA_EVENTS_PLUGIN = "ExyliaEvents";
    private static final String SERVICE_CLASS_NAME = "net.exylia.exyliaEvents.api.nametag.NameTagVisibilityService";
    private static final String SERVICE_METHOD = "shouldShowNametag";

    private final @NotNull NameTags plugin;
    private volatile @Nullable Object service;
    private volatile @Nullable Method serviceMethod;

    private ExyliaEventsVisibilityProvider(@NotNull NameTags plugin) {
        this.plugin = plugin;
    }

    public static @NotNull NameTagVisibilityProvider create(@NotNull NameTags plugin) {
        Plugin exyliaEvents = Bukkit.getPluginManager().getPlugin(EXYLIA_EVENTS_PLUGIN);
        if (exyliaEvents == null || !exyliaEvents.isEnabled()) {
            return PermissiveNameTagVisibilityProvider.INSTANCE;
        }

        ExyliaEventsVisibilityProvider provider = new ExyliaEventsVisibilityProvider(plugin);
        if (provider.resolveService()) {
            plugin.getLogger().info("ExyliaEvents detected, enabling team-aware nametag visibility.");
            return provider;
        }

        plugin.getLogger().warning("ExyliaEvents detected but nametag visibility service was unavailable, falling back to permissive visibility.");
        return PermissiveNameTagVisibilityProvider.INSTANCE;
    }

    @Override
    public boolean canSee(@NotNull Player viewer, @NotNull Player target) {
        if (!resolveService()) {
            return true;
        }

        try {
            return (boolean) serviceMethod.invoke(service, viewer, target);
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("Failed to query ExyliaEvents nametag visibility service: " + ex.getMessage());
            service = null;
            serviceMethod = null;
            return true;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean resolveService() {
        if (service != null && serviceMethod != null) {
            return true;
        }

        Plugin exyliaEvents = Bukkit.getPluginManager().getPlugin(EXYLIA_EVENTS_PLUGIN);
        if (exyliaEvents == null || !exyliaEvents.isEnabled()) {
            return false;
        }

        try {
            ClassLoader classLoader = exyliaEvents.getClass().getClassLoader();
            Class serviceClass = Class.forName(SERVICE_CLASS_NAME, true, classLoader);
            RegisteredServiceProvider registration = Bukkit.getServicesManager().getRegistration(serviceClass);
            if (registration == null) {
                return false;
            }

            Object provider = registration.getProvider();
            Method method = serviceClass.getMethod(SERVICE_METHOD, Player.class, Player.class);
            service = provider;
            serviceMethod = method;
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }
}
