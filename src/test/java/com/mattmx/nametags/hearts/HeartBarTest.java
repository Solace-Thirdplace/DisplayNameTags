package com.mattmx.nametags.hearts;

import com.mattmx.nametags.hearts.HeartBar.Kind;
import com.mattmx.nametags.hearts.HeartBar.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartBarTest {

    private static final Settings DEFAULTS = Settings.DEFAULTS;
    private static final Settings NO_EMPTY = new Settings(
            10, 2, false, "❤", "♥", "❤", "&4", "&c", "&7"
    );

    private static String dump(double health, double maxHealth, Settings settings) {
        return HeartBar.rows(HeartBar.slots(health, maxHealth, settings), settings.heartsPerRow())
                .stream()
                .map(row -> row.stream().map(HeartBarTest::ch).collect(Collectors.joining()))
                .collect(Collectors.joining("/"));
    }

    private static String ch(Kind kind) {
        return switch (kind) {
            case FULL -> "F";
            case HALF -> "H";
            case EMPTY -> "E";
        };
    }

    @ParameterizedTest
    @CsvSource({
            "6,  6,  FFF",
            "4,  6,  FFE",
            "1,  6,  HEE",
            "0,  6,  EEE",
            "20, 20, FFFFFFFFFF",
            "15, 20, FFFFFFFHEE",
            "19, 20, FFFFFFFFFH",
            "0,  20, EEEEEEEEEE",
            "30, 30, FFFFF/FFFFFFFFFF",
            "22, 30, FEEEE/FFFFFFFFFF",
            "15, 30, EEEEE/FFFFFFFHEE",
            "40, 40, FFFFFFFFFF/FFFFFFFFFF",
            "50, 80, FFFFFFFFFF/FFFFFFFFFF",
            "80, 80, FFFFFFFFFF/FFFFFFFFFF",
            "10, 80, EEEEEEEEEE/FFFFFEEEEE",
            "21, 21, H/FFFFFFFFFF",
            "8,  10, FFFFE"
    })
    void scalesToMaxHealthAndCapsAtTwoRows(double health, double maxHealth, String expected) {
        assertEquals(expected, dump(health, maxHealth, DEFAULTS));
    }

    @Test
    void belowTwentyDoesNotPadToTen() {
        assertEquals(3, HeartBar.slots(6, 6, DEFAULTS).size());
        assertFalse(dump(6, 6, DEFAULTS).contains("E"));
    }

    @Test
    void extraRowSitsAboveTheFirstTen() {
        List<List<Kind>> rows = HeartBar.rows(HeartBar.slots(30, 30, DEFAULTS), 10);
        assertEquals(2, rows.size());
        assertEquals("FFFFF", rows.get(0).stream().map(HeartBarTest::ch).collect(Collectors.joining()));
        assertEquals("FFFFFFFFFF", rows.get(1).stream().map(HeartBarTest::ch).collect(Collectors.joining()));
    }

    @Test
    void hidingEmptyHeartsDropsUnfilledSlots() {
        assertEquals("FF", dump(4, 6, NO_EMPTY));
        assertEquals("F/FFFFFFFFFF", dump(22, 30, NO_EMPTY));
        assertEquals("", dump(0, 20, NO_EMPTY));
    }

    @Test
    void invalidHealthIsTreatedAsEmptyBar() {
        // Unknown/negative current health still uses max-health slots.
        assertEquals("EEEEEEEEEE", dump(Double.NaN, 20, DEFAULTS));
        assertEquals("EEEEEEEEEE", dump(-4, 20, DEFAULTS));
        assertEquals("", dump(10, Double.NaN, DEFAULTS));
        assertEquals("", dump(10, Double.NEGATIVE_INFINITY, DEFAULTS));
        assertTrue(HeartBar.slots(10, 0, DEFAULTS).isEmpty());
        assertTrue(HeartBar.slots(10, -20, DEFAULTS).isEmpty());
    }

    @Test
    void paintUsesConfiguredIconsAndColors() {
        assertEquals("&4❤&4❤&7❤", HeartBar.paint(List.of(Kind.FULL, Kind.FULL, Kind.EMPTY), DEFAULTS));
        assertEquals("&c♥", HeartBar.paint(List.of(Kind.HALF), DEFAULTS));
    }

    @Test
    void compactJoinsRowsIntoOneLine() {
        assertEquals(
                HeartBar.paint(HeartBar.slots(30, 30, DEFAULTS), DEFAULTS),
                HeartBar.renderCompact(30, 30, DEFAULTS)
        );
        assertEquals(2, HeartBar.renderRows(30, 30, DEFAULTS).size());
        assertEquals(1, HeartBar.renderRows(6, 6, DEFAULTS).size());
    }

    @Test
    void standaloneTokenIsTrimAndCaseInsensitive() {
        assertTrue(HeartBar.isStandaloneToken("<hearts>"));
        assertTrue(HeartBar.isStandaloneToken("  <HEARTS>  "));
        assertFalse(HeartBar.isStandaloneToken("<white><hearts></white>"));
        assertFalse(HeartBar.isStandaloneToken("%healthbar_getbar%"));
    }

    @Test
    void ceilCurrentHealthMatchesOldExpansion() {
        // 15.1 HP displays as 8 full hearts (ceil to 16).
        assertEquals("FFFFFFFFEE", dump(15.1, 20, DEFAULTS));
    }
}
