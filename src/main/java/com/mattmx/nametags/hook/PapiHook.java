package com.mattmx.nametags.hook;

import com.mattmx.nametags.NameTags;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PapiHook {
    private static final @NotNull Pattern PLACEHOLDER_REGEX = Pattern.compile("%(?!rel_)[^%]+%");
    private static final @NotNull Pattern RELATIVE_PLACEHOLDER_REGEX = Pattern.compile("%[^%]+%");
    /**
     * Non-relational placeholders only; {@code %rel_...%} is left for
     * {@link #setRelationalPlaceholders} on outgoing packets.
     */
    private static final @NotNull Pattern NON_RELATIVE_PLACEHOLDER = Pattern.compile(
        "%(?!rel_)[^%]+%",
        Pattern.CASE_INSENSITIVE
    );

    public static boolean isPapi() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public static String setPlaceholders(Player one, String text) {
        if (!isPapi()) return text;

        Matcher matcher = NON_RELATIVE_PLACEHOLDER.matcher(text);
        String resetSuffix = NameTags.getInstance().getFormatter().placeholderExpansionResetSuffix();
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String expanded = PlaceholderAPI.setPlaceholders(one, matcher.group());
            matcher.appendReplacement(out, Matcher.quoteReplacement(expanded + resetSuffix));
        }
        if (out.length() == 0) {
            return text;
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public static Component setPlaceholders(Player one, Component text) {
        if (!isPapi()) return text;

        return text.replaceText(TextReplacementConfig.builder()
            .match(PLACEHOLDER_REGEX)
            .replacement((match, ctx) -> {
                String matchedText = match.group();
                String parsed = PlaceholderAPI.setPlaceholders(one, matchedText);
                return Component.text(parsed);
            })
            .build()
        );
    }

    public static Component setRelationalPlaceholders(Player one, Player two, Component text) {
        if (!isPapi()) return text;

        return text.replaceText(TextReplacementConfig.builder()
            .match(RELATIVE_PLACEHOLDER_REGEX)
            .replacement((match, ctx) -> {
                String matchedText = match.group();
                String parsed = PlaceholderAPI.setRelationalPlaceholders(one, two, matchedText);
                String resetSuffix = NameTags.getInstance().getFormatter().placeholderExpansionResetSuffix();
                return NameTags.getInstance().getFormatter().format(parsed + resetSuffix);
            })
            .build()
        );
    }

}
