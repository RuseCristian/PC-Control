package com.example.myapplication

enum class Platform { WINDOWS, LINUX }

data class PcButton(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val command: String,
    val color: Int, // Color as Argb Int
    val iconName: String = "Terminal",
    val requireConfirmation: Boolean = false,
    val runAsAdmin: Boolean = false
)

data class PcProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val ip: String,
    val mac: String,
    val user: String,
    val pass: String? = null,
    val privateKey: String? = null,
    val sshPort: Int = 22,
    val wolPort: Int = 9,
    val platform: Platform = Platform.WINDOWS,
    val customButtons: List<PcButton> = emptyList(),
    val hostname: String? = null
)

enum class DeviceStatus { ONLINE, OFFLINE, CHECKING }

enum class DragValue { Center, Start, End }
