package com.javimetallab.deadmanshot.mail

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Base64
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

enum class Security { SSL, STARTTLS }

class SmtpAccount(
    val host: String,
    val port: Int,
    val security: Security,
    val user: String,
    val password: String,
)

class SmtpException(message: String) : IOException(message)

/**
 * Just enough SMTP to hand one message to the user's own mail server, always
 * over TLS with the certificate and host name checked. No library: fewer
 * dependencies, and nothing F-Droid could object to.
 */
object SmtpClient {
    private const val TIMEOUT_MS = 30_000

    fun send(account: SmtpAccount, from: String, recipients: List<String>, message: ByteArray) {
        val plain = Socket().apply {
            connect(InetSocketAddress(account.host, account.port), TIMEOUT_MS)
            soTimeout = TIMEOUT_MS
        }
        var socket: Socket = plain
        try {
            if (account.security == Security.SSL) socket = tls(plain, account)
            var conn = Conn(socket)
            conn.expect(220)
            var caps = conn.ehlo()
            if (account.security == Security.STARTTLS) {
                if (caps.none { it.uppercase().startsWith("STARTTLS") }) {
                    throw SmtpException("${account.host} does not offer STARTTLS")
                }
                conn.command("STARTTLS", 220)
                socket = tls(plain, account)
                conn = Conn(socket)
                caps = conn.ehlo()
            }
            authenticate(conn, caps, account)
            conn.command("MAIL FROM:<$from>", 250)
            for (r in recipients) conn.command("RCPT TO:<$r>", 250, 251)
            conn.command("DATA", 354)
            conn.writeRaw(dotStuff(message))
            conn.writeRaw("\r\n.\r\n".toByteArray())
            conn.expect(250)
            runCatching { conn.command("QUIT", 221) }
        } finally {
            runCatching { socket.close() }
            runCatching { plain.close() }
        }
    }

    private fun tls(plain: Socket, account: SmtpAccount): SSLSocket {
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val s = factory.createSocket(plain, account.host, account.port, true) as SSLSocket
        s.sslParameters = s.sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
        s.soTimeout = TIMEOUT_MS
        s.startHandshake()
        if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(account.host, s.session)) {
            throw SSLPeerUnverifiedException("Certificate does not match ${account.host}")
        }
        return s
    }

    private fun authenticate(conn: Conn, caps: List<String>, account: SmtpAccount) {
        val auth = caps.firstOrNull { it.uppercase().startsWith("AUTH") }?.uppercase()
            ?: throw SmtpException("The server does not offer authentication")
        val b64 = Base64.getEncoder()
        if (" PLAIN" in auth || "=PLAIN" in auth) {
            val token = "\u0000${account.user}\u0000${account.password}".toByteArray(Charsets.UTF_8)
            conn.command("AUTH PLAIN " + b64.encodeToString(token), 235)
        } else if (" LOGIN" in auth || "=LOGIN" in auth) {
            conn.command("AUTH LOGIN", 334)
            conn.command(b64.encodeToString(account.user.toByteArray(Charsets.UTF_8)), 334)
            conn.command(b64.encodeToString(account.password.toByteArray(Charsets.UTF_8)), 235)
        } else {
            throw SmtpException("No supported authentication method ($auth)")
        }
    }

    /** Doubles a dot at the start of any line, and makes sure the message ends in CRLF. */
    fun dotStuff(message: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(message.size + 64)
        var lineStart = true
        for (b in message) {
            if (lineStart && b == '.'.code.toByte()) out.write('.'.code)
            out.write(b.toInt())
            lineStart = b == '\n'.code.toByte()
        }
        if (!lineStart) out.write("\r\n".toByteArray())
        return out.toByteArray()
    }

    private class Conn(socket: Socket) {
        private val input: InputStream = BufferedInputStream(socket.getInputStream())
        private val output: OutputStream = socket.getOutputStream()

        fun writeRaw(bytes: ByteArray) {
            output.write(bytes)
            output.flush()
        }

        fun command(cmd: String, vararg ok: Int): List<String> {
            writeRaw("$cmd\r\n".toByteArray(Charsets.UTF_8))
            return expect(*ok)
        }

        fun ehlo(): List<String> = command("EHLO localhost", 250).drop(1)

        /** Reads one reply (maybe several lines) and returns the text of each line. */
        fun expect(vararg ok: Int): List<String> {
            val lines = mutableListOf<String>()
            var code: Int
            while (true) {
                val line = readLine() ?: throw SmtpException("Connection closed by the server")
                if (line.length < 3) throw SmtpException("Unexpected reply: $line")
                code = line.substring(0, 3).toIntOrNull()
                    ?: throw SmtpException("Unexpected reply: $line")
                lines += line.drop(4)
                if (line.length == 3 || line[3] != '-') break
            }
            if (code !in ok) throw SmtpException("$code ${lines.joinToString(" ")}".trim())
            return lines
        }

        private fun readLine(): String? {
            val buf = ByteArrayOutputStream()
            while (true) {
                val b = input.read()
                if (b < 0) return if (buf.size() == 0) null else buf.toString(Charsets.UTF_8.name())
                if (b == '\n'.code) break
                if (b != '\r'.code) buf.write(b)
            }
            return buf.toString(Charsets.UTF_8.name())
        }
    }
}
