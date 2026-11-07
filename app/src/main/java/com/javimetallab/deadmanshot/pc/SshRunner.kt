package com.javimetallab.deadmanshot.pc

import com.javimetallab.deadmanshot.core.Store
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import java.io.ByteArrayOutputStream
import java.util.Base64

class SshException(message: String) : Exception(message)

data class SshResult(val exitCode: Int, val stdout: String, val stderr: String) {
    val ok: Boolean get() = exitCode == 0
}

/**
 * Thin wrapper around JSch. Trust-on-first-use: the server's host key is
 * remembered on the first connection; later connections must match or they
 * are rejected with a clear error.
 */
object SshRunner {
    /** For connecting, and the default for a command. */
    private const val TIMEOUT_MS = 15_000

    /**
     * Runs [command] and waits up to [timeoutMs] for it to end. Interrupting the
     * calling thread stops the wait and closes the connection.
     */
    fun run(
        host: String, port: Int, user: String, password: String, command: String,
        timeoutMs: Long = TIMEOUT_MS.toLong(),
    ): SshResult {
        val session = openSession(host, port, user, password)
        try {
            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            val stdout = ByteArrayOutputStream()
            val stderr = ByteArrayOutputStream()
            channel.outputStream = stdout
            channel.setErrStream(stderr)
            channel.connect(TIMEOUT_MS)
            val start = System.currentTimeMillis()
            while (!channel.isClosed) {
                if (System.currentTimeMillis() - start > timeoutMs) {
                    channel.disconnect()
                    throw SshException("Command timed out after ${timeoutMs / 1000} s")
                }
                Thread.sleep(50)
            }
            val code = channel.exitStatus
            channel.disconnect()
            return SshResult(
                code,
                stdout.toString("UTF-8"),
                stderr.toString("UTF-8"),
            )
        } finally {
            session.disconnect()
        }
    }

    /** Fire-and-forget: send the command, do not wait for it to finish. */
    fun runFireAndForget(host: String, port: Int, user: String, password: String, command: String) {
        val session = openSession(host, port, user, password)
        try {
            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            channel.connect(TIMEOUT_MS)
            Thread.sleep(500)
            channel.disconnect()
        } finally {
            session.disconnect()
        }
    }

    /**
     * Signs in with the app's own key when there is one (and with the password as a
     * fallback, if there is one too), or with the password alone.
     */
    private fun openSession(host: String, port: Int, user: String, password: String): Session {
        val jsch = JSch()
        jsch.hostKeyRepository = TofuHostKeyRepository(host, port)
        val privateKey = Store.sshPrivateKey()
        val publicKey = Store.snapshot().pc.sshPublicKey
        val methods = mutableListOf<String>()
        if (privateKey != null && publicKey.isNotBlank()) {
            jsch.addIdentity(
                "deadmanshot", privateKey.toByteArray(Charsets.US_ASCII),
                publicKey.toByteArray(Charsets.US_ASCII), null,
            )
            methods += "publickey"
        }
        // A single password attempt per connection: Windows locks the account after 10
        // failures in 10 minutes (and then refuses even the key), so never retry it.
        if (password.isNotEmpty()) methods += "password"
        if (methods.isEmpty()) throw SshException("No SSH key or password")
        val session = jsch.getSession(user, host, port)
        if (password.isNotEmpty()) session.setPassword(password)
        session.userInfo = AcceptAllPrompts(password)
        session.setConfig("StrictHostKeyChecking", "yes")
        session.setConfig("PreferredAuthentications", methods.joinToString(","))
        session.setConfig("MaxAuthTries", methods.size.toString())
        session.connect(TIMEOUT_MS)
        return session
    }

    /**
     * Creates the app's SSH key: RSA 3072, which every OpenSSH server accepts and
     * Android can generate without extra libraries. Returns (private PEM, public line).
     */
    fun generateKey(comment: String): Pair<String, String> {
        val pair = com.jcraft.jsch.KeyPair.genKeyPair(JSch(), com.jcraft.jsch.KeyPair.RSA, 3072)
        try {
            val prv = ByteArrayOutputStream().also { pair.writePrivateKey(it) }.toString("US-ASCII")
            val pub = ByteArrayOutputStream().also { pair.writePublicKey(it, comment) }.toString("US-ASCII").trim()
            return prv to pub
        } finally {
            pair.dispose()
        }
    }

    private class AcceptAllPrompts(private val password: String) : UserInfo {
        override fun getPassphrase(): String? = null
        override fun getPassword(): String = password
        override fun promptPassword(message: String?): Boolean = true
        override fun promptPassphrase(message: String?): Boolean = true
        override fun promptYesNo(message: String?): Boolean = true
        override fun showMessage(message: String?) {}
    }

    private class TofuHostKeyRepository(
        private val host: String,
        private val port: Int,
    ) : HostKeyRepository {
        override fun check(hostName: String, key: ByteArray): Int {
            val fingerprint = Base64.getEncoder().encodeToString(key)
            val saved = Store.savedSshHostKey(host, port)
            return when {
                saved == null -> {
                    Store.saveSshHostKey(host, port, fingerprint)
                    HostKeyRepository.OK
                }
                saved == fingerprint -> HostKeyRepository.OK
                else -> HostKeyRepository.CHANGED
            }
        }
        override fun add(hostkey: HostKey, ui: UserInfo?) {}
        override fun remove(hostName: String, type: String) {}
        override fun remove(hostName: String, type: String, key: ByteArray) {}
        override fun getKnownHostsRepositoryID(): String = "DeadManShot-TOFU"
        override fun getHostKey(): Array<HostKey> = emptyArray()
        override fun getHostKey(hostName: String?, type: String?): Array<HostKey> = emptyArray()
    }
}
