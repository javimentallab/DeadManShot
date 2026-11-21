package com.javimetallab.deadmanshot.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.javimetallab.deadmanshot.R
import com.javimetallab.deadmanshot.mail.Attachment
import com.javimetallab.deadmanshot.mail.Mime
import com.javimetallab.deadmanshot.mail.SmtpAccount
import com.javimetallab.deadmanshot.mail.SmtpClient
import java.io.IOException
import java.util.UUID

object Mailer {
    private val ADDRESS = Regex("^[^@\\s<>,;]+@[^@\\s<>,;]+\\.[^@\\s<>,;]+$")

    fun parseRecipients(raw: String): List<String> =
        raw.split(',', ';', ' ', '\n').map { it.trim() }.filter { it.isNotEmpty() }

    fun isAddress(s: String) = ADDRESS.matches(s)

    /** What a provider asks for instead of the account's usual password, if anything. */
    sealed interface AccountNote {
        /** Gmail, Yahoo, iCloud: an app password, which needs two-step verification first. */
        data class AppPassword(val provider: AppPasswordProvider) : AccountNote
        /** Proton: an SMTP token, only for custom-domain addresses on paid plans. */
        data class ProtonToken(val page: String) : AccountNote
        /** Outlook, Hotmail, Microsoft 365: OAuth only, which this app does not do. */
        data object MicrosoftOAuth : AccountNote
    }

    private val MICROSOFT_HOSTS = setOf(
        "smtp.office365.com", "smtp-mail.outlook.com", "smtp.outlook.com", "smtp.live.com", "smtp.hotmail.com",
    )

    fun accountNote(host: String): AccountNote? {
        Login.provider(host)?.let { return AccountNote.AppPassword(it) }
        return when (host.trim().lowercase()) {
            "smtp.protonmail.ch", "smtp.proton.me" -> AccountNote.ProtonToken("https://proton.me/support/smtp-submission")
            in MICROSOFT_HOSTS -> AccountNote.MicrosoftOAuth
            else -> null
        }
    }

    /** A sending error in words the user can act on, or the server's own words if not recognised. */
    fun explain(context: Context, error: String): String = when (Login.classify(error)) {
        SendFailure.MICROSOFT -> context.getString(R.string.err_microsoft)
        SendFailure.APP_PASSWORD -> context.getString(R.string.err_app_password)
        SendFailure.BAD_LOGIN -> context.getString(R.string.err_bad_login)
        SendFailure.OTHER -> error
    }

    /** What is missing before the switch can be trusted to send; empty when ready. */
    fun missing(context: Context, s: Snapshot): List<String> {
        val out = mutableListOf<String>()
        val to = parseRecipients(s.email.recipients)
        if (to.isEmpty() || !to.all(::isAddress)) out += context.getString(R.string.missing_recipient)
        if (s.email.subject.isBlank() && s.email.body.isBlank()) out += context.getString(R.string.missing_text)
        if (s.smtp.host.isBlank() || s.smtp.user.isBlank() || !s.smtp.hasPassword) {
            out += context.getString(R.string.missing_account)
        }
        return out
    }

    fun displayName(context: Context, uri: Uri): String =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull() ?: uri.lastPathSegment ?: uri.toString()

    /**
     * Sends the configured email. With [test], it goes to the sender's own
     * address instead, with the subject marked as a test.
     * Returns the names of attachments that could not be read (they are left out).
     */
    fun send(context: Context, s: Snapshot, test: Boolean): List<String> {
        val password = Store.password() ?: throw IOException(context.getString(R.string.missing_account))
        val from = s.smtp.from.ifBlank { s.smtp.user }
        val to = if (test) listOf(from) else parseRecipients(s.email.recipients)
        if (to.isEmpty()) throw IOException(context.getString(R.string.missing_recipient))

        val attachments = mutableListOf<Attachment>()
        val skipped = mutableListOf<String>()
        for (raw in s.email.attachments) {
            val uri = Uri.parse(raw)
            val name = displayName(context, uri)
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IOException("unreadable")
                val type = context.contentResolver.getType(uri) ?: "application/octet-stream"
                attachments += Attachment(name, type, bytes)
            } catch (e: Exception) {
                skipped += name
            }
        }

        val subject = if (test) context.getString(R.string.test_subject_prefix) + " " + s.email.subject
        else s.email.subject
        val message = Mime.build(
            from = from,
            to = to,
            subject = subject,
            body = s.email.body,
            attachments = attachments,
            dateMillis = System.currentTimeMillis(),
            boundary = "dms-" + UUID.randomUUID(),
            messageId = UUID.randomUUID().toString() + "@" + from.substringAfter('@'),
        )
        SmtpClient.send(
            SmtpAccount(s.smtp.host, s.smtp.port, s.smtp.security, s.smtp.user, Login.normalisePassword(s.smtp.host, password)),
            from, to, message
        )
        return skipped
    }
}
