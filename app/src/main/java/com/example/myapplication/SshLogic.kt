package com.example.myapplication

import com.trilead.ssh2.ChannelCondition
import com.trilead.ssh2.Connection

fun runSshCommand(profile: PcProfile, command: String, quiet: Boolean = false): String {
    val conn = Connection(profile.ip, profile.sshPort)
    
    return try {
        conn.connect(null, 5000, 5000)
        
        val authenticated = when {
            profile.privateKey != null -> {
                conn.authenticateWithPublicKey(profile.user, profile.privateKey.toCharArray(), null)
            }
            profile.pass != null -> {
                conn.authenticateWithPassword(profile.user, profile.pass)
            }
            else -> false
        }

        if (!authenticated) return "SSH Error: Authentication failed"

        val sess = conn.openSession()
        sess.requestDumbPTY() 
        
        val finalCommand = if (command.startsWith("ADMIN:")) {
            val actualCmd = command.removePrefix("ADMIN:")
            val escapedCmd = actualCmd.replace("'", "''")
            "powershell -Command \"Start-Process powershell -ArgumentList '-Command', '$escapedCmd' -Verb RunAs\""
        } else command
        
        sess.execCommand(finalCommand)

        val output = try { sess.stdout.bufferedReader().readText() } catch (e: Exception) { "" }
        val error = try { sess.stderr.bufferedReader().readText() } catch (e: Exception) { "" }
        
        sess.waitForCondition(ChannelCondition.EXIT_STATUS or ChannelCondition.CLOSED, 2000)
        val exitStatus = sess.exitStatus ?: 0
        sess.close()

        val ansiRegex = Regex("\u001B\\[[;?]*[0-9;]*[a-zA-Z]|\u001B][0-9];.*?\u0007")
        val cleanOutput = output.replace(ansiRegex, "").trim()
        val cleanError = error.replace(ansiRegex, "").trim()

        if (exitStatus != 0) {
            val msg = cleanError.ifEmpty { cleanOutput }.lineSequence()
                .filter { it.isNotBlank() }
                .lastOrNull() ?: "Command Failed"
            "Error: $msg"
        } else {
            if (quiet) "Success" 
            else {
                cleanOutput.lineSequence()
                    .filter { it.isNotBlank() }
                    .lastOrNull() ?: "Success"
            }
        }
    } catch (e: Exception) {
        "SSH Error: ${e.message}"
    } finally {
        conn.close()
    }
}
