package com.javimetallab.deadmanshot.core

/**
 * A provider that takes only an app password over SMTP. Creating one needs
 * two-step verification turned on first, or the provider does not offer it.
 */
enum class AppPasswordProvider(
    /** The mail service, as the user knows it. */
    val service: String,
    /** Whose account it is, for "there is no signing in with …". */
    val company: String,
    val twoStepPage: String,
    val appPasswordPage: String,
) {
    GMAIL(
        "Gmail", "Google",
        "https://myaccount.google.com/signinoptions/twosv",
        "https://myaccount.google.com/apppasswords",
    ),
    YAHOO(
        "Yahoo", "Yahoo",
        "https://login.yahoo.com/account/security",
        "https://login.yahoo.com/account/security/app-passwords",
    ),
    ICLOUD(
        "iCloud", "Apple",
        "https://account.apple.com/account/manage/section/security",
        "https://account.apple.com/account/manage/section/security",
    ),
}

/** Why a sending attempt failed, as far as the user can do something about it. */
enum class SendFailure {
    /** Microsoft no longer takes a password at all. */
    MICROSOFT,
    /** The server said in so many words that it wants an app password (534 5.7.9). */
    APP_PASSWORD,
    /** The user or the password was refused (535, or Gmail's 534 5.7.14). */
    BAD_LOGIN,
    OTHER;

    val isLogin: Boolean get() = this == APP_PASSWORD || this == BAD_LOGIN
}

/**
 * Account rules that do not need Android. No regex either: the JVM tests
 * would run Java's engine, not the phone's.
 */
object Login {
    fun provider(host: String): AppPasswordProvider? = when (host.trim().lowercase()) {
        "smtp.gmail.com", "smtp.googlemail.com" -> AppPasswordProvider.GMAIL
        "smtp.mail.yahoo.com" -> AppPasswordProvider.YAHOO
        "smtp.mail.me.com" -> AppPasswordProvider.ICLOUD
        else -> null
    }

    /** App passwords of those providers are 16 letters, shown in groups with spaces or hyphens. */
    fun looksLikeAppPassword(password: String): Boolean =
        password.filterNot { it == ' ' || it == '-' }
            .let { p -> p.length == 16 && p.all { it in 'a'..'z' || it in 'A'..'Z' } }

    /**
     * Google and Yahoo show app passwords with spaces, which are not part of
     * them; no app password of these three has a space in it.
     */
    fun normalisePassword(host: String, password: String): String =
        if (provider(host) != null) password.replace(" ", "") else password

    /** Sorts a server's error by what the user has to do about it. */
    fun classify(error: String): SendFailure {
        val e = error.trimStart()
        return when {
            "5.7.139" in e || "5.7.30" in e || "asic authentication is disabled" in e -> SendFailure.MICROSOFT
            "5.7.9" in e || "pplication-specific password" in e -> SendFailure.APP_PASSWORD
            e.startsWith("535") -> SendFailure.BAD_LOGIN
            e.startsWith("534") && "5.7.14" in e -> SendFailure.BAD_LOGIN
            else -> SendFailure.OTHER
        }
    }
}
