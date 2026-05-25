package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            var accentColor by remember { mutableStateOf(Color(0xFF00F5A0)) }
            var isDarkTheme by remember { mutableStateOf(true) }
            var backgroundColor by remember { mutableStateOf<Color?>(null) }

            PcControlTheme(
                darkTheme = isDarkTheme, 
                accentColor = accentColor,
                backgroundColor = backgroundColor
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainApp(
                        accentColor = accentColor,
                        onColorChange = { accentColor = it },
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { 
                            isDarkTheme = !isDarkTheme
                            backgroundColor = null 
                        },
                        currentBg = backgroundColor,
                        onBgChange = { backgroundColor = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    accentColor: Color,
    onColorChange: (Color) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    currentBg: Color?,
    onBgChange: (Color?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var profiles by remember { mutableStateOf(emptyList<PcProfile>()) }
    var selectedProfile by remember { mutableStateOf<PcProfile?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<PcProfile?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    
    val profileStatuses = remember { mutableStateMapOf<String, DeviceStatus>() }
    val profileHostnames = remember { mutableStateMapOf<String, String>() }
    var terminalProfileId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profiles) {
        while (true) {
            profiles.forEach { profile ->
                launch(Dispatchers.IO) {
                    val isOnline = try {
                        val socket = java.net.Socket()
                        socket.connect(java.net.InetSocketAddress(profile.ip, profile.sshPort), 1500)
                        socket.close()
                        true
                    } catch (e: Exception) {
                        false
                    }
                    
                    val newStatus = if (isOnline) DeviceStatus.ONLINE else DeviceStatus.OFFLINE
                    profileStatuses[profile.id] = newStatus
                    
                    if (isOnline && profileHostnames[profile.id] == null) {
                        val cmd = if (profile.platform == Platform.WINDOWS) "echo %COMPUTERNAME%" else "hostname"
                        val res = runSshCommand(profile, cmd)
                        if (!res.startsWith("SSH Error") && !res.startsWith("Error")) {
                            profileHostnames[profile.id] = res
                        }
                    } else if (!isOnline) {
                        profileHostnames.remove(profile.id)
                    }
                }
            }
            delay(5000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedProfile,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            }, label = "ScreenTransition"
        ) { targetProfile ->
            if (targetProfile == null) {
                ProfileListScreen(
                    profiles = profiles,
                    statuses = profileStatuses,
                    onProfileClick = { selectedProfile = it },
                    onAddClick = { 
                        editingProfile = null
                        showAddDialog = true 
                    },
                    onEditClick = { 
                        editingProfile = it
                        showAddDialog = true 
                    },
                    onSettingsClick = { showSettings = true },
                    onReorder = { from, to ->
                        val list = profiles.toMutableList()
                        val item = list.removeAt(from)
                        list.add(to, item)
                        profiles = list
                    },
                    onResult = { msg ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )
            } else {
                BackHandler { selectedProfile = null }
                ProfileDetailScreen(
                    profile = targetProfile,
                    status = profileStatuses[targetProfile.id] ?: DeviceStatus.CHECKING,
                    fetchedHostname = profileHostnames[targetProfile.id],
                    onBack = { selectedProfile = null },
                    onTerminalClick = { terminalProfileId = it },
                    onResult = { msg ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    },
                    onUpdateProfile = { updated ->
                        profiles = profiles.map { if (it.id == updated.id) updated else it }
                        selectedProfile = updated
                    }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )

        if (terminalProfileId != null) {
            val profile = profiles.find { it.id == terminalProfileId }
            if (profile != null) {
                TerminalScreen(
                    profile = profile,
                    onDismiss = { terminalProfileId = null }
                )
            }
        }
    }

    if (showAddDialog) {
        ProfileDialog(
            profile = editingProfile,
            onDismiss = { showAddDialog = false },
            onSave = { savedProfile ->
                profiles = if (editingProfile == null) {
                    profiles + savedProfile
                } else {
                    profiles.map { if (it.id == savedProfile.id) savedProfile else it }
                }
                showAddDialog = false
            },
            onDelete = { profileToDelete ->
                profiles = profiles.filter { it.id != profileToDelete.id }
                showAddDialog = false
            }
        )
    }

    if (showSettings) {
        SettingsSheet(
            isDarkTheme = isDarkTheme,
            onThemeToggle = onThemeToggle,
            currentAccent = accentColor,
            onColorChange = onColorChange,
            currentBg = currentBg,
            onBgChange = onBgChange,
            onDismiss = { showSettings = false }
        )
    }
}
