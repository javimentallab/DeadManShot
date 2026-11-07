package com.javimetallab.deadmanshot

import com.javimetallab.deadmanshot.mail.Attachment
import com.javimetallab.deadmanshot.mail.Mime
import com.javimetallab.deadmanshot.mail.SmtpClient
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class MailTest {
    @Test fun asciiSubjectIsLeftAlone() {
        assertEquals("Hello there", Mime.encodeHeader("Hello there"))
    }

    @Test fun nonAsciiSubjectRoundTrips() {
        val subject = "Si lees esto, algo me ha pasado: ábrelo ya, por favor, es importante ñ€"
        val encoded = Mime.encodeHeader(subject)
        val words = encoded.split("\r\n ")
        assertTrue(words.size > 1)
        words.forEach { assertTrue(it.length <= 75) }
        val decoded = words.joinToString("") {
            String(Base64.getDecoder().decode(it.removePrefix("=?UTF-8?B?").removeSuffix("?=")), Charsets.UTF_8)
        }
        assertEquals(subject, decoded)
    }

    @Test fun filenameIsPercentEncoded() {
        assertEquals("carta%20para%20Mar%C3%ADa.pdf", Mime.percentEncode("carta para María.pdf"))
    }

    @Test fun messageHasBodyAndAttachment() {
        val msg = String(
            Mime.build(
                from = "a@example.com", to = listOf("b@example.com", "c@example.com"),
                subject = "Hola", body = "Línea 1\nLínea 2",
                attachments = listOf(Attachment("x.txt", "text/plain", "contenido".toByteArray())),
                dateMillis = 0, boundary = "BOUND", messageId = "id@example.com",
            ),
            Charsets.US_ASCII
        )
        assertTrue(msg.contains("To: <b@example.com>, <c@example.com>\r\n"))
        assertTrue(msg.contains("Content-Type: multipart/mixed; boundary=\"BOUND\"\r\n"))
        assertTrue(msg.contains(Base64.getEncoder().encodeToString("Línea 1\r\nLínea 2".toByteArray())))
        assertTrue(msg.contains("filename*=UTF-8''x.txt"))
        assertTrue(msg.contains(Base64.getEncoder().encodeToString("contenido".toByteArray())))
        assertTrue(msg.trimEnd().endsWith("--BOUND--"))
        // Nothing but CRLF line endings.
        assertEquals(msg.count { it == '\n' }, Regex("\r\n").findAll(msg).count())
    }

    @Test fun dotStuffing() {
        assertArrayEquals(
            "a\r\n..b\r\n..\r\n".toByteArray(),
            SmtpClient.dotStuff(".\r\n".let { "a\r\n.b\r\n$it" }.toByteArray()).let {
                // ".b" -> "..b" and the lone "." -> ".."
                it
            }.let { String(it).toByteArray() }
        )
        assertArrayEquals("x\r\n".toByteArray(), SmtpClient.dotStuff("x".toByteArray()))
    }
}
