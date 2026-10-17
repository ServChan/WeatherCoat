package org.lts.weathercoat.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CoatsTest {
    @Test
    void whiteIsNotTinted() {
        assertFalse(Coats.isTinted(-1));
        assertFalse(Coats.isTinted(0x00FFFFFF));
    }

    @Test
    void dyeColorsAreTinted() {
        assertTrue(Coats.isTinted(0xFFB02E26));
        assertTrue(Coats.isTinted(0xFFA06540));
        assertTrue(Coats.isTinted(0xFFFFFFFE));
    }
}
