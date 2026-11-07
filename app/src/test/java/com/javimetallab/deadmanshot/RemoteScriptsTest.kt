package com.javimetallab.deadmanshot

import com.javimetallab.deadmanshot.pc.PcActions
import com.javimetallab.deadmanshot.pc.PcOs
import com.javimetallab.deadmanshot.pc.RemoteScripts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RemoteScriptsTest {
    private fun win(p: String) = RemoteScripts.pathLooksSafe(PcOs.WINDOWS, p)
    private fun linux(p: String) = RemoteScripts.pathLooksSafe(PcOs.LINUX, p)

    @Test fun safeWindowsPaths() {
        listOf(
            "C:\\Users\\jose\\Desktop\\test",
            "C:\\Users\\jose\\Desktop\\test\\",
            "D:\\Backups\\keys\\key.bin",
            "D:/Backups/keys",
            "C:\\Users\\jose\\Desktop\\it's \$ odd \u2018quoted\u2019 [x]",
        ).forEach { assertTrue(it, win(it)) }
    }

    @Test fun unsafeWindowsPaths() {
        listOf(
            "", "/", "C:", "C:\\", "C:\\Users", "C:\\Users\\jose", "C:\\Users\\jose\\",
            "D:\\Backups", "C:\\Windows\\System32", "C:\\Program Files\\App",
            "C:\\ProgramData\\x", "C:\\a\\..\\..", "C:\\a\\.\\b", "C:\\a\\\\b",
            "C:\\a\\b\\*", "C:\\a\\b?", "relative\\path\\here", "\\\\server\\share\\dir",
            "/home/jose/test",
        ).forEach { assertFalse(it, win(it)) }
    }

    @Test fun safeLinuxPaths() {
        listOf(
            "/home/jose/test", "/home/jose/test/", "/mnt/data/keys/key.bin",
            "/srv/secret", "/root/test", "/home/jose/it's \$ odd \"q\" [x]",
        ).forEach { assertTrue(it, linux(it)) }
    }

    @Test fun unsafeLinuxPaths() {
        listOf(
            "", "/", "/home", "/home/jose", "/home/jose/", "/mnt", "/etc/ssh", "/usr/local/x",
            "/var/lib/x", "/boot/efi", "/home/jose/../..", "/home/./jose/x", "/home//jose/x",
            "/home/jose/*", "relative/path/here", "C:\\Users\\jose\\Desktop\\test",
        ).forEach { assertFalse(it, linux(it)) }
    }

    @Test fun windowsCommandFitsCmdExe() {
        // Windows OpenSSH runs commands through cmd.exe, which stops at 8191 characters.
        val cmd = RemoteScripts.wipe(PcOs.WINDOWS, "C:\\Users\\jose\\Desktop\\" + "x".repeat(200), true, true)
        assertTrue(cmd.length.toString(), cmd.length < 8000)
    }

    @Test fun summaryIsParsed() {
        assertEquals(3 to 1, PcActions.parseSummary("wiped=3 left=1\r\n"))
        assertEquals(0 to 0, PcActions.parseSummary(""))
    }

    /**
     * Writes the commands to build/wipe-commands so they can be tried on a real
     * machine. -Dwipe.base sets the folder they point at.
     */
    @Test fun dumpWipeCommands() {
        val dir = File("build/wipe-commands").apply { mkdirs() }
        val winBase = System.getProperty("wipe.win") ?: "C:\\dms-test"
        val linBase = System.getProperty("wipe.linux") ?: "/tmp/dms-test"
        for ((os, base, sep) in listOf(
            Triple(PcOs.WINDOWS, winBase, "\\"), Triple(PcOs.LINUX, linBase, "/"),
        )) {
            val tag = os.name.lowercase()
            fun dump(name: String, path: String, allowFile: Boolean, rec: Boolean) =
                File(dir, "$tag-$name.txt").writeText(RemoteScripts.wipe(os, base + sep + path, allowFile, rec))
            dump("rec", "rec", false, true)
            dump("flat", "flat", false, false)
            dump("key", "key.bin", true, true)
            dump("notfolder", "key2.bin", false, true)
            dump("missing", "nope", false, true)
            dump("odd", "it's \$ odd \u2018q\u2019 [x]", false, true)
        }
        File(dir, "windows-cmd.txt").writeText(
            RemoteScripts.userCommand(
                PcOs.WINDOWS,
                "Write-Output \"hola \$env:USERNAME\"\r\nWrite-Output 'line 2 with ''quotes'' & \u2018odd\u2019 %PATH%'\r\ncmd /c exit 3",
            )
        )
        File(dir, "windows-cmd-ok.txt").writeText(RemoteScripts.userCommand(PcOs.WINDOWS, "Get-Date | Out-Null\nWrite-Output fine"))
        File(dir, "linux-cmd.txt").writeText(
            RemoteScripts.userCommand(
                PcOs.LINUX,
                "echo \"hola \$USER\"\necho 'line 2 with \"quotes\" & \u2018odd\u2019 \$HOME'\nexit 3",
            )
        )
        File(dir, "linux-cmd-ok.txt").writeText(RemoteScripts.userCommand(PcOs.LINUX, "date >/dev/null\necho fine"))
    }
}
