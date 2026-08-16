package com.mattmx.nametags.config;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginConditionalsTest {

    private static final Predicate<String> NOTHING_INSTALLED = (name) -> false;
    private static final Predicate<String> PVP_TOGGLE_INSTALLED = Set.of("PvPToggle")::contains;

    private static final String NETWORK_LINE =
        "&b%player_ping%ms &7| &f &e%objective_score_{Deaths}%<ifplugin:PvPToggle> &7| &f%pvptoggle_icon%</ifplugin>";

    @Test
    void keepsContentWhenPluginIsPresent() {
        assertEquals(
            "&b%player_ping%ms &7| &f &e%objective_score_{Deaths}% &7| &f%pvptoggle_icon%",
            PluginConditionals.strip(NETWORK_LINE, PVP_TOGGLE_INSTALLED)
        );
    }

    @Test
    void removesSegmentWhenPluginIsAbsent() {
        assertEquals(
            "&b%player_ping%ms &7| &f &e%objective_score_{Deaths}%",
            PluginConditionals.strip(NETWORK_LINE, NOTHING_INSTALLED)
        );
    }

    @Test
    void handlesMultipleConditionalsIndependently() {
        String line = "a<ifplugin:PvPToggle>b</ifplugin>c<ifplugin:Missing>d</ifplugin>e";

        assertEquals("abce", PluginConditionals.strip(line, PVP_TOGGLE_INSTALLED));
        assertEquals("ace", PluginConditionals.strip(line, NOTHING_INSTALLED));
    }

    @Test
    void leavesTextWithoutConditionalsUntouched() {
        assertEquals("", PluginConditionals.strip("", NOTHING_INSTALLED));
        assertEquals("<red>%player_name%</red>", PluginConditionals.strip("<red>%player_name%</red>", NOTHING_INSTALLED));
    }

    @Test
    void tagNameIsCaseInsensitive() {
        assertEquals("hi", PluginConditionals.strip("<IfPlugin:PvPToggle>hi</IFPLUGIN>", PVP_TOGGLE_INSTALLED));
    }

    @Test
    void trimsWhitespaceAroundPluginName() {
        assertEquals("hi", PluginConditionals.strip("<ifplugin: PvPToggle >hi</ifplugin>", PVP_TOGGLE_INSTALLED));
    }

    @Test
    void emptyContentCollapsesToNothing() {
        assertEquals("ab", PluginConditionals.strip("a<ifplugin:PvPToggle></ifplugin>b", PVP_TOGGLE_INSTALLED));
        assertEquals("ab", PluginConditionals.strip("a<ifplugin:Missing></ifplugin>b", NOTHING_INSTALLED));
    }

    @Test
    void unknownNamesWithColonsOrSpacesAreTreatedAsAbsentPlugins() {
        assertEquals("a", PluginConditionals.strip("a<ifplugin:My Cool:Plugin>b</ifplugin>", NOTHING_INSTALLED));
        assertEquals(
            "ab",
            PluginConditionals.strip("a<ifplugin:My Cool:Plugin>b</ifplugin>", (name) -> name.equals("My Cool:Plugin"))
        );
    }

    @Test
    void malformedOpenTagIsRenderedAsIs() {
        // No closing '>' - not a tag at all.
        assertEquals(
            "a<ifplugin:PvPToggle b</ifplugin>",
            PluginConditionals.strip("a<ifplugin:PvPToggle b</ifplugin>", PVP_TOGGLE_INSTALLED)
        );

        // Empty plugin name.
        assertEquals(
            "a<ifplugin:>b</ifplugin>",
            PluginConditionals.strip("a<ifplugin:>b</ifplugin>", PVP_TOGGLE_INSTALLED)
        );
    }

    @Test
    void malformedCloseTagIsRenderedAsIs() {
        // Missing closing tag entirely.
        assertEquals(
            "a<ifplugin:PvPToggle>b",
            PluginConditionals.strip("a<ifplugin:PvPToggle>b", PVP_TOGGLE_INSTALLED)
        );
        assertEquals(
            "a<ifplugin:Missing>b",
            PluginConditionals.strip("a<ifplugin:Missing>b", NOTHING_INSTALLED)
        );

        // Stray closing tag with no opening tag.
        assertEquals("a</ifplugin>b", PluginConditionals.strip("a</ifplugin>b", PVP_TOGGLE_INSTALLED));
    }

    @Test
    void nestedConditionalsAreRenderedAsIs() {
        String nested = "<ifplugin:PvPToggle>a<ifplugin:PvPToggle>b</ifplugin>c</ifplugin>";

        assertEquals(nested, PluginConditionals.strip(nested, PVP_TOGGLE_INSTALLED));
        assertEquals(nested, PluginConditionals.strip(nested, NOTHING_INSTALLED));
    }

    @Test
    void wellFormedConditionalAfterAMalformedOneStillResolves() {
        assertEquals(
            "<ifplugin:>x b",
            PluginConditionals.strip("<ifplugin:>x <ifplugin:PvPToggle>b</ifplugin>", PVP_TOGGLE_INSTALLED)
        );
    }
}
