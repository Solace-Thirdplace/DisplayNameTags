package com.mattmx.nametags.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectatorStandinPolicyTest {

    @Test
    void survivalViewerSeesASpectatorsStandinWhenEnabled() {
        assertTrue(SpectatorStandinPolicy.showStandinTo(true, true, false, false));
    }

    @Test
    void otherSpectatorsDoNotGetASecondHead() {
        assertFalse(SpectatorStandinPolicy.showStandinTo(true, true, true, false));
    }

    @Test
    void theOwnerDoesNotSeeTheirOwnStandin() {
        assertFalse(SpectatorStandinPolicy.showStandinTo(true, true, false, true));
        assertFalse(SpectatorStandinPolicy.showStandinTo(true, true, true, true));
    }

    @Test
    void disabledFeatureNeverShowsAStandin() {
        assertFalse(SpectatorStandinPolicy.showStandinTo(false, true, false, false));
    }

    @Test
    void nonSpectatorsHaveNoStandin() {
        assertFalse(SpectatorStandinPolicy.showStandinTo(true, false, false, false));
    }
}
