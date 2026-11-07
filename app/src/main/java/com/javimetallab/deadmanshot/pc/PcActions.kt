package com.javimetallab.deadmanshot.pc

import android.content.Context
import com.javimetallab.deadmanshot.R
import com.javimetallab.deadmanshot.core.Notifications
import com.javimetallab.deadmanshot.core.PcConfig
import com.javimetallab.deadmanshot.core.Store
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runInterruptible
import java.io.IOException

/** Runs the configured PC actions when the switch fires. */
object PcActions {
    /**
     * WorkManager stops a worker after ten minutes. Everything (the wait after
     * WoL and every SSH command) has to fit in this budget.
     */
    private const val BUDGET_MS = 9 * 60_000L + 30_000L
    private const val MIN_COMMAND_MS = 20_000L
    private const val SHUTDOWN_MS = 20_000L

    fun pathLooksSafe(path: String, os: PcOs = PcOs.WINDOWS): Boolean =
        RemoteScripts.pathLooksSafe(os, path)

    suspend fun runOnFire(context: Context, pc: PcConfig) {
        val budgetEnd = System.currentTimeMillis() + BUDGET_MS
        fun remaining() = (budgetEnd - System.currentTimeMillis()).coerceAtLeast(MIN_COMMAND_MS)
        val log = StringBuilder()
        val password = Store.sshPassword().orEmpty()

        if (pc.enableWol && Wol.isMacValid(pc.mac)) {
            runCatching { Wol.send(pc.mac, pc.broadcast.ifBlank { "255.255.255.255" }) }
                .onSuccess { log.appendLine(context.getString(R.string.pc_log_wol_ok)) }
                .onFailure { log.appendLine(context.getString(R.string.pc_log_wol_failed, it.message ?: "")) }
            // Leave at least two minutes for the SSH commands.
            val wait = (pc.wolWaitSeconds.coerceAtLeast(0) * 1000L).coerceAtMost(BUDGET_MS - 120_000L)
            delay(wait)
        }

        if (needsSsh(pc) && (pc.sshHost.isBlank() || pc.sshUser.isBlank() || (!pc.hasSshPassword && pc.sshPublicKey.isBlank()))) {
            log.appendLine(context.getString(R.string.pc_log_ssh_missing))
            report(context, log.toString())
            return
        }

        val res = context.resources
        fun files(n: Int) = res.getQuantityString(R.plurals.pc_files_wiped, n, n)
        fun subfolders(n: Int) = res.getQuantityString(R.plurals.pc_subfolders_left, n, n)

        suspend fun wipe(path: String, allowFile: Boolean, recursive: Boolean, ok: Int, failed: Int) {
            val cmd = RemoteScripts.wipe(pc.os, path, allowFile, recursive)
            val (r, error) = ssh(pc, password, cmd, remaining())
            log.appendLine(
                when {
                    r == null -> context.getString(failed, error)
                    r.ok -> {
                        val (wiped, left) = parseSummary(r.stdout)
                        context.getString(ok, files(wiped), subfolders(left))
                    }
                    else -> context.getString(failed, describe(context, r))
                }
            )
        }

        if (pc.enableCommand && pc.command.isNotBlank()) {
            val (r, error) = ssh(pc, password, RemoteScripts.userCommand(pc.os, pc.command), remaining())
            log.appendLine(
                when {
                    r == null -> context.getString(R.string.pc_log_cmd_failed, error)
                    r.ok -> context.getString(R.string.pc_log_cmd_ok, outputTail(r))
                    else -> context.getString(R.string.pc_log_cmd_exit, r.exitCode, outputTail(r))
                }.trim()
            )
        }

        if (pc.enableDestroyKey) {
            if (pathLooksSafe(pc.keyPath, pc.os)) {
                wipe(pc.keyPath, allowFile = true, recursive = true, R.string.pc_log_key_ok, R.string.pc_log_key_failed)
            } else {
                log.appendLine(context.getString(R.string.pc_log_key_unsafe))
            }
        }

        if (pc.enableShred) {
            if (pathLooksSafe(pc.folder, pc.os)) {
                wipe(pc.folder, allowFile = false, recursive = pc.includeSubdirs,
                    R.string.pc_log_shred_ok, R.string.pc_log_shred_failed)
            } else {
                log.appendLine(context.getString(R.string.pc_log_shred_unsafe))
            }
        }

        if (pc.enableSshShutdown) {
            log.appendLine(shutdown(context, pc, password))
        }

        report(context, log.toString())
    }

    private suspend fun shutdown(context: Context, pc: PcConfig, password: String): String =
        when (pc.os) {
            PcOs.WINDOWS -> try {
                runInterruptible {
                    SshRunner.runFireAndForget(
                        pc.sshHost, pc.sshPort, pc.sshUser, password, RemoteScripts.shutdown(pc.os)
                    )
                }
                context.getString(R.string.pc_log_shutdown_ok)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                context.getString(R.string.pc_log_shutdown_failed, e.message ?: "")
            }
            PcOs.LINUX -> {
                val (r, error) = ssh(pc, password, RemoteScripts.shutdown(pc.os), SHUTDOWN_MS)
                when {
                    r == null -> context.getString(R.string.pc_log_shutdown_failed, error)
                    // -1: the machine went down before answering, which is what we want.
                    r.ok || r.exitCode == -1 -> context.getString(R.string.pc_log_shutdown_ok)
                    else -> context.getString(
                        R.string.pc_log_shutdown_failed,
                        (r.stdout + "\n" + r.stderr).trim().lines().takeLast(3).joinToString("\n"),
                    )
                }
            }
        }

    /** Runs one command; the result, or null and the reason it could not run at all. */
    private suspend fun ssh(
        pc: PcConfig, password: String, cmd: String, timeoutMs: Long,
    ): Pair<SshResult?, String> =
        try {
            runInterruptible {
                SshRunner.run(pc.sshHost, pc.sshPort, pc.sshUser, password, cmd, timeoutMs)
            } to ""
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null to (e.message ?: e.javaClass.simpleName)
        }

    private fun describe(context: Context, r: SshResult): String {
        val err = r.stderr.trim().lines().filter { it.isNotBlank() }.take(5).joinToString("\n")
        return when (r.exitCode) {
            RemoteScripts.EXIT_NOT_FOUND -> context.getString(R.string.pc_log_not_found)
            RemoteScripts.EXIT_LINK -> context.getString(R.string.pc_log_is_link)
            RemoteScripts.EXIT_NOT_FOLDER -> context.getString(R.string.pc_log_not_folder)
            RemoteScripts.EXIT_NO_TOOL -> context.getString(R.string.pc_log_no_shred)
            RemoteScripts.EXIT_PARTIAL -> {
                val (wiped, _) = parseSummary(r.stdout)
                context.getString(
                    R.string.pc_log_partial,
                    context.resources.getQuantityString(R.plurals.pc_partial_files, wiped, wiped), err,
                )
            }
            else -> context.getString(R.string.pc_log_exit, r.exitCode, err)
        }
    }

    private val SUMMARY = Regex("wiped=(\\d+) left=(\\d+)")

    fun parseSummary(stdout: String): Pair<Int, Int> =
        SUMMARY.find(stdout)?.let { it.groupValues[1].toInt() to it.groupValues[2].toInt() } ?: (0 to 0)

    private fun needsSsh(pc: PcConfig): Boolean =
        pc.enableCommand || pc.enableDestroyKey || pc.enableShred || pc.enableSshShutdown

    /** The last few lines the command printed, short enough for a notification. */
    fun outputTail(r: SshResult): String =
        (r.stdout + "\n" + r.stderr).lines().map { it.trimEnd() }.filter { it.isNotBlank() }
            .takeLast(5).joinToString("\n").takeLast(600)

    private fun report(context: Context, body: String) {
        Notifications.showPcResult(
            context,
            context.getString(R.string.pc_notification_title),
            body.trim().ifEmpty { context.getString(R.string.pc_log_nothing) },
        )
    }

    @Throws(IOException::class)
    fun testSsh(host: String, port: Int, user: String, password: String): SshResult {
        return SshRunner.run(host, port, user, password, "echo ok")
    }
}
