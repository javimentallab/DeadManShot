package com.javimetallab.deadmanshot

import com.javimetallab.deadmanshot.pc.Wol
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WolTest {
    @Test fun validMacs() {
        assertTrue(Wol.isMacValid("aa:bb:cc:dd:ee:ff"))
        assertTrue(Wol.isMacValid("AA-BB-CC-DD-EE-FF"))
        assertTrue(Wol.isMacValid("aabbccddeeff"))
    }

    @Test fun invalidMacs() {
        assertFalse(Wol.isMacValid(""))
        assertFalse(Wol.isMacValid("aa:bb:cc:dd:ee"))
        assertFalse(Wol.isMacValid("zz:bb:cc:dd:ee:ff"))
    }
}
