package com.mattmx.nametags.packet;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TameableOwnerHiderTest {

    @Test
    void detectsPresentOwnerUuid() {
        assertTrue(TameableOwnerHider.isPresentOwnerUuid(
                Optional.of(UUID.fromString("e0aedff2-fe97-411c-a9fd-ff25843a12f4"))));
    }

    @Test
    void ignoresEmptyOptional() {
        assertFalse(TameableOwnerHider.isPresentOwnerUuid(Optional.empty()));
    }

    @Test
    void ignoresNonUuidOptionalsAndOtherTypes() {
        assertFalse(TameableOwnerHider.isPresentOwnerUuid(Optional.of(12)));
        assertFalse(TameableOwnerHider.isPresentOwnerUuid(Optional.of("owner")));
        assertFalse(TameableOwnerHider.isPresentOwnerUuid((byte) 0x05));
        assertFalse(TameableOwnerHider.isPresentOwnerUuid(null));
    }
}
