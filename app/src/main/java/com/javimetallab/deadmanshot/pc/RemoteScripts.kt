package com.javimetallab.deadmanshot.pc

import java.util.Base64

/** The operating system of the machine the PC actions run on. */
enum class PcOs { WINDOWS, LINUX }

/**
 * The commands sent over SSH, one version per system. Both wipe scripts behave
 * the same way:
 *
 * - every file is overwritten in place twice (zeros, then random data), each
 *   pass flushed to disk, and only then deleted;
 * - a file that cannot be overwritten is left where it is and reported, never
 *   deleted unwiped;
 * - links are not followed;
 * - without `recursive`, subfolders are left alone; folders are only removed
 *   once empty;
 * - they print `wiped=N left=M` (M = subfolders not entered) and exit with 0
 *   only if everything went, or with one of the `EXIT_*` codes.
 *
 * The path travels base64-encoded, so no character in it can break the script.
 */
object RemoteScripts {
    const val EXIT_PARTIAL = 1
    const val EXIT_NOT_FOUND = 2
    const val EXIT_LINK = 3
    const val EXIT_NOT_FOLDER = 4
    const val EXIT_NO_TOOL = 5

    // --- Path checks ---

    private val WIN_DRIVE_PATH = Regex("^[A-Za-z]:[\\\\/].*")
    private val WIN_BAD_CHARS = Regex("[*?<>|\"\\u0000-\\u001F]")
    private val WIN_SYSTEM_DIRS = setOf(
        "windows", "program files", "program files (x86)", "programdata",
        "\$recycle.bin", "system volume information", "recovery", "boot",
    )
    private val LINUX_SYSTEM_DIRS = setOf(
        "bin", "boot", "dev", "etc", "lib", "lib32", "lib64", "libx32", "lost+found",
        "proc", "run", "sbin", "snap", "sys", "usr", "var",
    )

    /**
     * Refuses anything that could wipe far more than meant: roots, folders one
     * level below a root, a whole user profile (C:\Users\name, /home/name),
     * system folders, relative paths, `.`/`..` components and wildcards.
     */
    fun pathLooksSafe(os: PcOs, path: String): Boolean = when (os) {
        PcOs.WINDOWS -> windowsPathLooksSafe(path)
        PcOs.LINUX -> linuxPathLooksSafe(path)
    }

    private fun windowsPathLooksSafe(path: String): Boolean {
        val trimmed = path.trim().trimEnd('\\', '/')
        if (!WIN_DRIVE_PATH.matches("$trimmed\\")) return false
        if (WIN_BAD_CHARS.containsMatchIn(trimmed)) return false
        val parts = trimmed.split('\\', '/')
        // parts[0] is the drive ("C:").
        val dirs = parts.drop(1)
        if (dirs.any { it.isBlank() || it == "." || it == ".." }) return false
        if (dirs.any { it.lowercase() in WIN_SYSTEM_DIRS }) return false
        if (dirs.size < 2) return false
        if (dirs[0].equals("Users", ignoreCase = true) && dirs.size < 3) return false
        return true
    }

    private fun linuxPathLooksSafe(path: String): Boolean {
        val trimmed = path.trim().trimEnd('/')
        if (!trimmed.startsWith("/")) return false
        if (trimmed.any { it == '\u0000' || it == '\n' || it == '*' || it == '?' }) return false
        val dirs = trimmed.drop(1).split('/')
        if (dirs.any { it.isEmpty() || it == "." || it == ".." }) return false
        if (dirs[0] in LINUX_SYSTEM_DIRS) return false
        if (dirs.size < 2) return false
        if (dirs[0] == "home" && dirs.size < 3) return false
        return true
    }

    // --- Wiping ---

    fun wipe(os: PcOs, path: String, allowFile: Boolean, recursive: Boolean): String {
        val b64 = Base64.getEncoder().encodeToString(path.toByteArray(Charsets.UTF_8))
        return when (os) {
            PcOs.WINDOWS -> windowsWipe(b64, allowFile, recursive)
            PcOs.LINUX -> linuxWipe(b64, allowFile, recursive)
        }
    }

    private fun windowsWipe(b64: String, allowFile: Boolean, recursive: Boolean): String {
        val r = if (recursive) "1" else "0"
        val f = if (allowFile) "1" else "0"
        val script = """
            ${'$'}ErrorActionPreference='Stop';${'$'}ProgressPreference='SilentlyContinue'
            try{[Console]::OutputEncoding=[Text.Encoding]::UTF8}catch{}
            ${'$'}p=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('$b64'))
            ${'$'}rec=$r -eq 1;${'$'}w=0;${'$'}left=0;${'$'}bad=New-Object Collections.Generic.List[string]
            ${'$'}rng=[Security.Cryptography.RandomNumberGenerator]::Create();${'$'}buf=New-Object byte[] 1048576
            ${'$'}RP=[IO.FileAttributes]::ReparsePoint
            function WF(${'$'}x){try{
            [IO.File]::SetAttributes(${'$'}x,'Normal');${'$'}len=(New-Object IO.FileInfo ${'$'}x).Length
            ${'$'}s=[IO.File]::Open(${'$'}x,'Open','Write','None')
            try{foreach(${'$'}k in 0,1){${'$'}s.Position=0;${'$'}d=[long]0
            while(${'$'}d -lt ${'$'}len){${'$'}n=[int][Math]::Min(${'$'}buf.Length,${'$'}len-${'$'}d)
            if(${'$'}k -eq 0){[Array]::Clear(${'$'}buf,0,${'$'}n)}else{${'$'}rng.GetBytes(${'$'}buf)}
            ${'$'}s.Write(${'$'}buf,0,${'$'}n);${'$'}d+=${'$'}n}
            ${'$'}s.Flush(${'$'}true)}}finally{${'$'}s.Dispose()}
            [IO.File]::Delete(${'$'}x);${'$'}script:w++}catch{${'$'}bad.Add(${'$'}x+': '+${'$'}_.Exception.Message)}}
            function WD(${'$'}x){try{${'$'}c=@(Get-ChildItem -LiteralPath ${'$'}x -Force)}catch{${'$'}bad.Add(${'$'}x+': '+${'$'}_.Exception.Message);return}
            foreach(${'$'}i in ${'$'}c){if(${'$'}i.Attributes -band ${'$'}RP){${'$'}bad.Add(${'$'}i.FullName+': link, not followed');continue}
            if(${'$'}i.PSIsContainer){if(${'$'}rec){WD ${'$'}i.FullName}else{${'$'}script:left++}}else{WF ${'$'}i.FullName}}
            try{if(@(Get-ChildItem -LiteralPath ${'$'}x -Force).Count -eq 0){[IO.Directory]::Delete(${'$'}x)}}catch{${'$'}bad.Add(${'$'}x+': '+${'$'}_.Exception.Message)}}
            if(-not(Test-Path -LiteralPath ${'$'}p)){[Console]::Error.WriteLine('Not found: '+${'$'}p);exit $EXIT_NOT_FOUND}
            ${'$'}it=Get-Item -LiteralPath ${'$'}p -Force
            if(${'$'}it.Attributes -band ${'$'}RP){[Console]::Error.WriteLine('Is a link: '+${'$'}p);exit $EXIT_LINK}
            if(${'$'}it.PSIsContainer){WD ${'$'}p}elseif($f -eq 1){WF ${'$'}p}else{[Console]::Error.WriteLine('Not a folder: '+${'$'}p);exit $EXIT_NOT_FOLDER}
            [Console]::Out.WriteLine('wiped='+${'$'}w+' left='+${'$'}left)
            if(${'$'}bad.Count -gt 0){${'$'}bad|Select-Object -First 5|ForEach-Object{[Console]::Error.WriteLine(${'$'}_)};exit $EXIT_PARTIAL}
            exit 0
        """.trimIndent()
        return powershell(script)
    }

    /** Needs bash, base64 and shred (coreutils), which every mainstream distribution has. */
    private fun linuxWipe(b64: String, allowFile: Boolean, recursive: Boolean): String {
        val r = if (recursive) "1" else "0"
        val f = if (allowFile) "1" else "0"
        val script = """
            set -u
            p=${'$'}(printf %s '$b64' | base64 -d)
            rec=$r; allowfile=$f; w=0; left=0; bad=0; errs=''
            note() { bad=${'$'}((bad+1)); if [ "${'$'}bad" -le 5 ]; then errs="${'$'}errs${'$'}1"${'$'}'\n'; fi; }
            command -v shred >/dev/null 2>&1 || { echo 'shred not found' >&2; exit $EXIT_NO_TOOL; }
            wf() {
              chmod u+w -- "${'$'}1" 2>/dev/null
              if e=${'$'}(shred --random-source=/dev/zero -n 1 -- "${'$'}1" 2>&1 </dev/null) &&
                 e=${'$'}(shred -n 1 -u -- "${'$'}1" 2>&1 </dev/null); then
                w=${'$'}((w+1))
              else
                note "${'$'}1: ${'$'}e"
              fi
            }
            wd() {
              local d="${'$'}1" i
              if [ ! -r "${'$'}d" ] || [ ! -x "${'$'}d" ]; then note "${'$'}d: permission denied"; return; fi
              while IFS= read -r -d '' i; do
                if [ -L "${'$'}i" ]; then note "${'$'}i: link, not followed"
                elif [ -d "${'$'}i" ]; then
                  if [ "${'$'}rec" = 1 ]; then wd "${'$'}i"; else left=${'$'}((left+1)); fi
                elif [ -f "${'$'}i" ]; then wf "${'$'}i"
                else note "${'$'}i: not a regular file"
                fi
              done < <(find "${'$'}d" -mindepth 1 -maxdepth 1 -print0 2>/dev/null)
              if [ -z "${'$'}(ls -A -- "${'$'}d" 2>/dev/null)" ]; then
                rmdir -- "${'$'}d" 2>/dev/null || note "${'$'}d: could not remove"
              fi
            }
            if [ -L "${'$'}p" ]; then echo "Is a link: ${'$'}p" >&2; exit $EXIT_LINK; fi
            if [ ! -e "${'$'}p" ]; then echo "Not found: ${'$'}p" >&2; exit $EXIT_NOT_FOUND; fi
            if [ -d "${'$'}p" ]; then wd "${'$'}p"
            elif [ "${'$'}allowfile" = 1 ]; then wf "${'$'}p"
            else echo "Not a folder: ${'$'}p" >&2; exit $EXIT_NOT_FOLDER
            fi
            echo "wiped=${'$'}w left=${'$'}left"
            if [ "${'$'}bad" -gt 0 ]; then printf %s "${'$'}errs" >&2; exit $EXIT_PARTIAL; fi
            exit 0
        """.trimIndent() + "\n"
        val encoded = Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_8))
        return "echo $encoded | base64 -d | bash -s"
    }

    private fun powershell(script: String): String {
        val encoded = Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))
        return "powershell -NoProfile -NonInteractive -EncodedCommand $encoded"
    }

    // --- The user's own command ---

    /**
     * Runs what the user typed, any number of lines: in PowerShell on Windows,
     * in bash on Linux. It travels base64-encoded, so no character in it is
     * mangled by the shell that receives it over SSH.
     */
    fun userCommand(os: PcOs, command: String): String = when (os) {
        PcOs.WINDOWS -> powershell(
            command.replace("\r\n", "\n") +
                "\nif (\$LASTEXITCODE) { exit \$LASTEXITCODE } elseif (-not \$?) { exit 1 }"
        )
        PcOs.LINUX -> {
            val encoded = Base64.getEncoder().encodeToString(
                (command.replace("\r\n", "\n") + "\n").toByteArray(Charsets.UTF_8)
            )
            "echo $encoded | base64 -d | bash -s"
        }
    }

    // --- Shutting down ---

    /**
     * Windows: forced shutdown, sent without waiting. Linux: needs passwordless
     * sudo for systemctl (or polkit allowing it); the answer is read, so a
     * refusal is reported.
     */
    fun shutdown(os: PcOs): String = when (os) {
        PcOs.WINDOWS -> "shutdown /s /f /t 0"
        PcOs.LINUX -> "sudo -n systemctl poweroff 2>&1 || systemctl poweroff 2>&1"
    }
}
