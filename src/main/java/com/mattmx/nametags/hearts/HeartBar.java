package com.mattmx.nametags.hearts;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Renders a vanilla-style heart bar from current health and max health.
 * <p>
 * Slot count follows {@code ceil(maxHealth / 2)}, capped at
 * {@code heartsPerRow * maxRows} (default 10 × 2). Extra hearts (slots 11–20)
 * are returned as the first row so they sit above the first ten on a nametag,
 * matching the vanilla HUD. A player whose max is 20 or below gets one row.
 */
public final class HeartBar {

    public static final @NotNull String TOKEN = "<hearts>";
    public static final @NotNull Pattern TOKEN_PATTERN = Pattern.compile("(?i)<hearts>");

    public enum Kind {
        FULL, HALF, EMPTY
    }

    public record Settings(
            int heartsPerRow,
            int maxRows,
            boolean showEmpty,
            @NotNull String fullIcon,
            @NotNull String halfIcon,
            @NotNull String emptyIcon,
            @NotNull String fullColor,
            @NotNull String halfColor,
            @NotNull String emptyColor
    ) {
        public static final @NotNull Settings DEFAULTS = new Settings(
                10, 2, true, "❤", "♥", "❤", "&4", "&c", "&7"
        );

        public static @NotNull Settings fromConfig(@NotNull FileConfiguration config) {
            ConfigurationSection section = config.getConfigurationSection("hearts");
            if (section == null) {
                return DEFAULTS;
            }

            int perRow = Math.max(1, section.getInt("hearts-per-row", DEFAULTS.heartsPerRow));
            // Product cap: two rows. Raising this in config is ignored.
            int maxRows = Math.min(2, Math.max(1, section.getInt("max-rows", DEFAULTS.maxRows)));
            boolean showEmpty = section.getBoolean("show-empty", DEFAULTS.showEmpty);

            ConfigurationSection icons = section.getConfigurationSection("icons");
            ConfigurationSection colors = section.getConfigurationSection("colors");

            return new Settings(
                    perRow,
                    maxRows,
                    showEmpty,
                    icon(icons, "full", DEFAULTS.fullIcon),
                    icon(icons, "half", DEFAULTS.halfIcon),
                    icon(icons, "empty", DEFAULTS.emptyIcon),
                    color(colors, "full", DEFAULTS.fullColor),
                    color(colors, "half", DEFAULTS.halfColor),
                    color(colors, "empty", DEFAULTS.emptyColor)
            );
        }

        private static @NotNull String icon(@Nullable ConfigurationSection icons, @NotNull String key, @NotNull String fallback) {
            if (icons == null) {
                return fallback;
            }
            String value = icons.getString(key, fallback);
            return value == null || value.isEmpty() ? fallback : value;
        }

        private static @NotNull String color(@Nullable ConfigurationSection colors, @NotNull String key, @NotNull String fallback) {
            if (colors == null) {
                return fallback;
            }
            String value = colors.getString(key, fallback);
            return value == null || value.isEmpty() ? fallback : value;
        }
    }

    private HeartBar() {
    }

    public static boolean isStandaloneToken(@NotNull String resolvedLine) {
        return resolvedLine.trim().equalsIgnoreCase(TOKEN);
    }

    public static @NotNull List<Kind> slots(double health, double maxHealth, @NotNull Settings settings) {
        int cap = settings.heartsPerRow() * settings.maxRows();
        int maxSlots = slotCount(maxHealth, cap);
        int remaining = finiteHealthPoints(health);

        List<Kind> out = new ArrayList<>(maxSlots);
        for (int i = 0; i < maxSlots; i++) {
            if (remaining >= 2) {
                out.add(Kind.FULL);
                remaining -= 2;
            } else if (remaining > 0) {
                out.add(Kind.HALF);
                remaining = 0;
            } else if (settings.showEmpty()) {
                out.add(Kind.EMPTY);
            }
        }
        return out;
    }

    /**
     * Extra hearts first (top of the nametag), then the first ten (closest to the head).
     * One list when the player has 10 or fewer slots.
     */
    public static @NotNull List<List<Kind>> rows(@NotNull List<Kind> slots, int heartsPerRow) {
        if (slots.isEmpty()) {
            return List.of();
        }
        if (slots.size() <= heartsPerRow) {
            return List.of(List.copyOf(slots));
        }
        List<Kind> bottom = List.copyOf(slots.subList(0, heartsPerRow));
        List<Kind> top = List.copyOf(slots.subList(heartsPerRow, slots.size()));
        return List.of(top, bottom);
    }

    public static @NotNull String paint(@NotNull List<Kind> row, @NotNull Settings settings) {
        StringBuilder sb = new StringBuilder(row.size() * 4);
        for (Kind kind : row) {
            switch (kind) {
                case FULL -> sb.append(settings.fullColor()).append(settings.fullIcon());
                case HALF -> sb.append(settings.halfColor()).append(settings.halfIcon());
                case EMPTY -> sb.append(settings.emptyColor()).append(settings.emptyIcon());
            }
        }
        return sb.toString();
    }

    public static @NotNull List<String> renderRows(double health, double maxHealth, @NotNull Settings settings) {
        List<List<Kind>> grouped = rows(slots(health, maxHealth, settings), settings.heartsPerRow());
        List<String> painted = new ArrayList<>(grouped.size());
        for (List<Kind> row : grouped) {
            String text = paint(row, settings);
            if (!text.isEmpty()) {
                painted.add(text);
            }
        }
        return painted;
    }

    public static @NotNull String renderCompact(double health, double maxHealth, @NotNull Settings settings) {
        return paint(slots(health, maxHealth, settings), settings);
    }

    public static @NotNull List<String> renderRows(@NotNull Player player, @NotNull Settings settings) {
        return renderRows(healthOf(player), maxHealthOf(player), settings);
    }

    public static @NotNull String renderCompact(@NotNull Player player, @NotNull Settings settings) {
        return renderCompact(healthOf(player), maxHealthOf(player), settings);
    }

    static int slotCount(double maxHealth, int cap) {
        if (!Double.isFinite(maxHealth) || maxHealth <= 0 || cap <= 0) {
            return 0;
        }
        long slots = (long) Math.ceil(maxHealth / 2.0);
        if (slots <= 0) {
            return 0;
        }
        return (int) Math.min(slots, cap);
    }

    static int finiteHealthPoints(double health) {
        if (!Double.isFinite(health) || health <= 0) {
            return 0;
        }
        return (int) Math.ceil(health);
    }

    static double healthOf(@NotNull Player player) {
        try {
            return player.getHealth();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return 0;
        }
    }

    static double maxHealthOf(@NotNull Player player) {
        AttributeInstance inst = player.getAttribute(Attribute.MAX_HEALTH);
        if (inst == null) {
            return 20.0;
        }
        return inst.getValue();
    }
}
