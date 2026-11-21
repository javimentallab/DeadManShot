package com.javimetallab.deadmanshot

import com.javimetallab.deadmanshot.core.AppPasswordProvider
import com.javimetallab.deadmanshot.core.Login
import com.javimetallab.deadmanshot.core.SendFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginTest {
    @Test fun providersByHost() {
        assertEquals(AppPasswordProvider.GMAIL, Login.provider("smtp.gmail.com"))
        assertEquals(AppPasswordProvider.GMAIL, Login.provider(" SMTP.Gmail.com "))
        assertEquals(AppPasswordProvider.GMAIL, Login.provider("smtp.googlemail.com"))
        assertEquals(AppPasswordProvider.YAHOO, Login.provider("smtp.mail.yahoo.com"))
        assertEquals(AppPasswordProvider.ICLOUD, Login.provider("smtp.mail.me.com"))
        assertNull(Login.provider("smtp.protonmail.ch"))
        assertNull(Login.provider("smtp.office365.com"))
        assertNull(Login.provider("mail.example.com"))
        assertNull(Login.provider(""))
    }

    @Test fun everyProviderHasBothPages() {
        for (p in AppPasswordProvider.entries) {
            assertTrue(p.name, p.twoStepPage.startsWith("https://"))
            assertTrue(p.name, p.appPasswordPage.startsWith("https://"))
        }
        assertEquals("https://myaccount.google.com/signinoptions/twosv", AppPasswordProvider.GMAIL.twoStepPage)
        assertEquals("https://myaccount.google.com/apppasswords", AppPasswordProvider.GMAIL.appPasswordPage)
    }

    @Test fun appPasswordShapes() {
        assertTrue(Login.looksLikeAppPassword("abcdefghijklmnop"))
        assertTrue(Login.looksLikeAppPassword("abcd efgh ijkl mnop")) // Google and Yahoo
        assertTrue(Login.looksLikeAppPassword("abcd-efgh-ijkl-mnop")) // Apple
        assertTrue(Login.looksLikeAppPassword(" ABCD efgh-IJKL mnop "))
    }

    @Test fun notAppPasswords() {
        assertFalse(Login.looksLikeAppPassword(""))
        assertFalse(Login.looksLikeAppPassword("abcd efgh ijkl mno")) // 15 letters
        assertFalse(Login.looksLikeAppPassword("abcdefghijklmnopq")) // 17 letters
        assertFalse(Login.looksLikeAppPassword("Summer2026!Summer")) // a usual password
        assertFalse(Login.looksLikeAppPassword("abcd1fgh2jkl3nop")) // 16 characters, with digits
        assertFalse(Login.looksLikeAppPassword("abcd_efgh_ijkl_mnop"))
        assertFalse(Login.looksLikeAppPassword("ábcdefghijklmnop"))
    }

    @Test fun spacesAreDroppedOnlyForAppPasswordProviders() {
        assertEquals("abcdefghijklmnop", Login.normalisePassword("smtp.gmail.com", "abcd efgh ijkl mnop"))
        assertEquals("abcdefghijklmnop", Login.normalisePassword("smtp.mail.yahoo.com", "abcd efgh ijkl mnop"))
        assertEquals("abcd-efgh-ijkl-mnop", Login.normalisePassword("smtp.mail.me.com", "abcd-efgh-ijkl-mnop"))
        assertEquals("my pass word", Login.normalisePassword("mail.example.com", "my pass word"))
    }

    @Test fun gmailWrongPassword() {
        val e = "535 5.7.8 Username and Password not accepted. For more information, go to " +
            "5.7.8 https://support.google.com/mail/?p=BadCredentials a1-20020a05"
        assertEquals(SendFailure.BAD_LOGIN, Login.classify(e))
        assertTrue(Login.classify(e).isLogin)
    }

    @Test fun gmailWantsAppPassword() {
        val e = "534 5.7.9 Application-specific password required. For more information, go to " +
            "5.7.9 https://support.google.com/mail/?p=InvalidSecondFactor"
        assertEquals(SendFailure.APP_PASSWORD, Login.classify(e))
        assertTrue(Login.classify(e).isLogin)
    }

    @Test fun gmailWebLoginRequired() {
        val e = "534 5.7.14 <https://accounts.google.com/signin/continue?sarp=1> " +
            "5.7.14 Please log in via your web browser and then try again."
        assertEquals(SendFailure.BAD_LOGIN, Login.classify(e))
    }

    @Test fun yahooAndIcloudRefusals() {
        assertEquals(SendFailure.BAD_LOGIN, Login.classify("535 5.7.0 (#AUTH005) Too many bad auth attempts."))
        assertEquals(SendFailure.BAD_LOGIN, Login.classify("535 5.7.8 Authentication credentials invalid"))
        assertEquals(SendFailure.BAD_LOGIN, Login.classify("  535 Authentication failed"))
    }

    @Test fun microsoftIsNotALoginProblem() {
        val e = "535 5.7.139 Authentication unsuccessful, basic authentication is disabled."
        assertEquals(SendFailure.MICROSOFT, Login.classify(e))
        assertFalse(Login.classify(e).isLogin)
        assertEquals(SendFailure.MICROSOFT, Login.classify("535 5.7.3 Authentication unsuccessful 5.7.30"))
    }

    @Test fun otherErrorsAreLeftAlone() {
        for (e in listOf(
            "Connection closed by the server",
            "smtp.example.com does not offer STARTTLS",
            "550 5.1.1 The email account that you tried to reach does not exist",
            "534 5.7.11 Encryption required for requested authentication mechanism",
            "java.net.UnknownHostException",
            "",
        )) {
            assertEquals(e, SendFailure.OTHER, Login.classify(e))
            assertFalse(e, Login.classify(e).isLogin)
        }
    }
}
