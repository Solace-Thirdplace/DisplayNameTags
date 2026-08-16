package com.mattmx.nametags.config;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inline conditional wrappers for nametag line text.
 * <p>
 * Syntax: {@code <ifplugin:PluginName>...</ifplugin>}
 * <ul>
 *     <li>If the named plugin is installed, the wrapper tags are removed and the
 *     inner content is kept.</li>
 *     <li>If it is not installed, the wrapper <em>and</em> its content are removed.</li>
 * </ul>
 * Conditionals may not be nested. Anything malformed (missing {@code >}, missing
 * closing tag, empty plugin name, nesting) is rendered verbatim so that the
 * misconfiguration is visible in game rather than silently swallowing text.
 * <p>
 * "Installed" means {@link org.bukkit.plugin.PluginManager#getPlugin(String)} returns
 * non-null, i.e. presence, not {@code isEnabled()}. Presence is stable for the lifetime
 * of the server, whereas the enabled flag flickers during startup/reload ordering, and
 * this result is cached; a plugin cannot appear or disappear without a server restart.
 */
public final class PluginConditionals {

    private static final @NotNull Pattern OPEN_TAG = Pattern.compile("<ifplugin:([^<>]*)>", Pattern.CASE_INSENSITIVE);
    private static final @NotNull Pattern CLOSE_TAG = Pattern.compile("</ifplugin>", Pattern.CASE_INSENSITIVE);

    private static final @NotNull Map<String, Boolean> PRESENCE_CACHE = new ConcurrentHashMap<>();

    private PluginConditionals() {
    }

    /**
     * Resolves every conditional in the given line against the plugins installed on this
     * server. Safe to call on the tag render path; results of plugin lookups are cached.
     */
    public static @NotNull String apply(@NotNull String line) {
        return strip(line, PluginConditionals::isPluginPresent);
    }

    /**
     * Pure implementation of the conditional stripping, decoupled from Bukkit so it can be
     * exercised directly.
     *
     * @param input         the raw line
     * @param pluginPresent returns true if the named plugin is installed
     * @return the line with all well-formed conditionals resolved
     */
    public static @NotNull String strip(@NotNull String input, @NotNull Predicate<String> pluginPresent) {
        if (input.isEmpty() || input.indexOf('<') < 0) {
            return input;
        }

        Matcher open = OPEN_TAG.matcher(input);

        if (!open.find()) {
            return input;
        }

        StringBuilder out = new StringBuilder(input.length());
        int cursor = 0;

        do {
            int openStart = open.start();
            int openEnd = open.end();
            String name = open.group(1).trim();

            out.append(input, cursor, openStart);

            Matcher close = CLOSE_TAG.matcher(input);
            boolean hasClose = close.find(openEnd);

            // Malformed: no plugin name, or no closing tag anywhere after this one.
            if (name.isEmpty() || !hasClose) {
                out.append(input, openStart, openEnd);
                cursor = openEnd;
                continue;
            }

            String content = input.substring(openEnd, close.start());
            int afterClose = close.end();

            // Nested conditionals are unsupported - emit the whole region verbatim.
            if (OPEN_TAG.matcher(content).find()) {
                out.append(input, openStart, afterClose);
                cursor = afterClose;
                continue;
            }

            if (pluginPresent.test(name)) {
                out.append(content);
            }

            cursor = afterClose;
        } while (cursor < input.length() && open.find(cursor));

        out.append(input, cursor, input.length());

        return out.toString();
    }

    private static boolean isPluginPresent(@NotNull String name) {
        return PRESENCE_CACHE.computeIfAbsent(name, (key) -> Bukkit.getPluginManager().getPlugin(key) != null);
    }

    /**
     * Drops the cached plugin lookups. Called on config reload so a lookup that ran before
     * a plugin finished loading cannot stick around forever.
     */
    public static void clearCache() {
        PRESENCE_CACHE.clear();
    }
}
