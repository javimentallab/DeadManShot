package com.javimetallab.deadmanshot.mail

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

class Attachment(val name: String, val mimeType: String, val bytes: ByteArray)

/** Builds a plain-text message with attachments, ready for SMTP DATA (CRLF lines). */
object Mime {
    private const val CRLF = "\r\n"

    fun build(
        from: String,
        to: List<String>,
        subject: String,
        body: String,
        attachments: List<Attachment>,
        dateMillis: Long,
        boundary: String,
        messageId: String,
    ): ByteArray {
        val date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateMillis), ZoneId.systemDefault())
        val sb = StringBuilder()
        fun line(s: String = "") = sb.append(s).append(CRLF)

        line("From: <$from>")
        line("To: " + to.joinToString(", ") { "<$it>" })
        line("Subject: " + encodeHeader(subject))
        line("Date: " + DateTimeFormatter.RFC_1123_DATE_TIME.format(date))
        line("Message-ID: <$messageId>")
        line("MIME-Version: 1.0")
        line("Content-Type: multipart/mixed; boundary=\"$boundary\"")
        line()
        line("--$boundary")
        line("Content-Type: text/plain; charset=UTF-8")
        line("Content-Transfer-Encoding: base64")
        line()
        line(base64(body.replace("\r\n", "\n").replace("\n", CRLF).toByteArray(Charsets.UTF_8)))
        for (a in attachments) {
            line("--$boundary")
            line("Content-Type: ${a.mimeType}; name=\"${encodeHeader(a.name).replace("\"", "")}\"")
            line("Content-Disposition: attachment; filename*=UTF-8''${percentEncode(a.name)}")
            line("Content-Transfer-Encoding: base64")
            line()
            line(base64(a.bytes))
        }
        line("--$boundary--")
        return sb.toString().toByteArray(Charsets.US_ASCII)
    }

    private fun base64(bytes: ByteArray): String =
        Base64.getMimeEncoder(76, CRLF.toByteArray()).encodeToString(bytes)

    /** RFC 2047: plain ASCII stays as it is; anything else becomes UTF-8 encoded words. */
    fun encodeHeader(text: String): String {
        if (text.all { it.code in 0x20..0x7E } && !text.contains("=?")) return text
        val words = mutableListOf<String>()
        val chunk = StringBuilder()
        var bytes = 0
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val s = String(Character.toChars(cp))
            val n = s.toByteArray(Charsets.UTF_8).size
            // 45 bytes of UTF-8 make 60 of base64: each word stays under 75 characters.
            if (bytes + n > 45) {
                words += encodedWord(chunk.toString())
                chunk.clear()
                bytes = 0
            }
            chunk.append(s)
            bytes += n
            i += Character.charCount(cp)
        }
        if (chunk.isNotEmpty()) words += encodedWord(chunk.toString())
        return words.joinToString("$CRLF ")
    }

    private fun encodedWord(s: String): String =
        "=?UTF-8?B?" + Base64.getEncoder().encodeToString(s.toByteArray(Charsets.UTF_8)) + "?="

    /** RFC 2231 value for filename*. */
    fun percentEncode(s: String): String {
        val ok = "!#$&+-.^_`|~"
        val sb = StringBuilder()
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xFF
            if (c in 'a'.code..'z'.code || c in 'A'.code..'Z'.code || c in '0'.code..'9'.code ||
                ok.indexOf(c.toChar()) >= 0
            ) sb.append(c.toChar()) else sb.append('%').append("%02X".format(c))
        }
        return sb.toString()
    }
}
