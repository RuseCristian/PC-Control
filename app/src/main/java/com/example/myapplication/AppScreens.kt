package com.example.myapplication

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    profiles: List<PcProfile>,
    statuses: Map<String, DeviceStatus>,
    onProfileClick: (PcProfile) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (PcProfile) -> Unit,
    onSettingsClick: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    onResult: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var editingProfileId by remember { mutableStateOf<String?>(null) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val itemPositions = remember { mutableStateMapOf<String, Offset>() }
    var listVersion by remember { mutableStateOf(0) }
    
    var showWakeAllConfirm by remember { mutableStateOf(false) }
    var showOffAllConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text("PC CONTROL", 
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    ) 
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Tune, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Devices",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showWakeAllConfirm = true }) { 
                        Text("WAKE ALL", style = MaterialTheme.typography.labelSmall) 
                    }
                    
                    TextButton(onClick = { showOffAllConfirm = true }, 
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { 
                        Text("OFF ALL", style = MaterialTheme.typography.labelSmall) 
                    }
                }
            }
            
            if (showWakeAllConfirm) {
                AlertDialog(
                    onDismissRequest = { showWakeAllConfirm = false },
                    title = { Text("Confirm Wake All") },
                    text = { Text("Are you sure you want to send Wake-on-LAN packets to all devices?") },
                    confirmButton = {
                        Button(onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                profiles.forEach { sendWakeOnLan(it.mac, it.ip, it.wolPort) }
                                withContext(Dispatchers.Main) { onResult("Wake-on-LAN sent to all devices") }
                            }
                            showWakeAllConfirm = false
                        }) { Text("Wake All") }
                    },
                    dismissButton = { TextButton(onClick = { showWakeAllConfirm = false }) { Text("Cancel") } }
                )
            }

            if (showOffAllConfirm) {
                AlertDialog(
                    onDismissRequest = { showOffAllConfirm = false },
                    title = { Text("Confirm Shutdown All") },
                    text = { Text("Are you sure you want to send shutdown commands to all devices?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    profiles.forEach { profile ->
                                        val cmd = if (profile.platform == Platform.WINDOWS) "shutdown /s /f /t 0" else "sudo shutdown -h now"
                                        runSshCommand(profile, cmd, true)
                                    }
                                    withContext(Dispatchers.Main) { onResult("Shutdown command sent to all devices") }
                                }
                                showOffAllConfirm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Shutdown All") }
                    },
                    dismissButton = { TextButton(onClick = { showOffAllConfirm = false }) { Text("Cancel") } }
                )
            }
            
            if (profiles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No devices added yet", color = Color.Gray)
                }
            }

            key(listVersion) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { editingProfileId = null }
                        },
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(profiles, key = { _, p -> p.id }) { index, profile ->
                        val isDragging = draggedId == profile.id
                        val isEditingMe = editingProfileId == profile.id
                        
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { coords ->
                                    itemPositions[profile.id] = coords.positionInParent()
                                }
                                .zIndex(if (isDragging) 10f else 1f)
                                .offset {
                                    if (isDragging) {
                                        IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                    } else IntOffset.Zero
                                }
                                .pointerInput(profiles, editingProfileId) {
                                    detectTapGestures(
                                        onLongPress = { editingProfileId = profile.id },
                                        onTap = {
                                            if (isEditingMe) {
                                                editingProfileId = null
                                            } else {
                                                onProfileClick(profile)
                                                editingProfileId = null
                                            }
                                        }
                                    )
                                }
                                .pointerInput(profiles, editingProfileId) {
                                    if (isEditingMe) {
                                        detectDragGestures(
                                            onDragStart = { 
                                                draggedId = profile.id
                                                dragOffset = Offset.Zero
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffset += amount
                                                
                                                val currentPos = (itemPositions[profile.id] ?: Offset.Zero) + dragOffset
                                                
                                                for (i in 0 until profiles.size) {
                                                    val otherProfile = profiles[i]
                                                    if (otherProfile.id == profile.id) continue
                                                    val otherPos = itemPositions[otherProfile.id] ?: continue
                                                    
                                                    if (abs(currentPos.y - otherPos.y) < 50) {
                                                        val fromIndex = profiles.indexOfFirst { it.id == profile.id }
                                                        if (fromIndex != -1) {
                                                            onReorder(fromIndex, i)
                                                            listVersion++
                                                            dragOffset = currentPos - otherPos
                                                        }
                                                        break
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                draggedId = null
                                                dragOffset = Offset.Zero
                                            },
                                            onDragCancel = {
                                                draggedId = null
                                                dragOffset = Offset.Zero
                                            }
                                        )
                                    }
                                }
                        ) {
                            SwipeableDeviceCard(
                                profile = profile,
                                status = statuses[profile.id] ?: DeviceStatus.CHECKING,
                                onClick = {
                                    if (editingProfileId != null) {
                                        editingProfileId = null
                                    } else {
                                        onProfileClick(profile)
                                    }
                                },
                                onEdit = { 
                                    onEditClick(profile)
                                    editingProfileId = null
                                },
                                onWake = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val res = sendWakeOnLan(profile.mac, profile.ip, profile.wolPort)
                                        withContext(Dispatchers.Main) { onResult(res) }
                                    }
                                },
                                onShutdown = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val cmd = if (profile.platform == Platform.WINDOWS) "shutdown /s /f /t 0" else "sudo shutdown -h now"
                                        val res = runSshCommand(profile, cmd)
                                        withContext(Dispatchers.Main) { onResult(res) }
                                    }
                                },
                                showEditIcon = isEditingMe && !isDragging,
                                isSwipable = !isEditingMe
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    profile: PcProfile,
    status: DeviceStatus,
    fetchedHostname: String?,
    onBack: () -> Unit,
    onTerminalClick: (String) -> Unit,
    onResult: (String) -> Unit,
    onUpdateProfile: (PcProfile) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var showConfirmDialog by remember { mutableStateOf<PcButton?>(null) }
    var showCustomCommandDialog by remember { mutableStateOf(false) }
    var showEditButtonDialog by remember { mutableStateOf<PcButton?>(null) }
    var showAddButtonDialog by remember { mutableStateOf(false) }
    
    var editingButtonId by remember { mutableStateOf<String?>(null) }
    
    val defaultButtons = listOf(
        PcButton(id = "WAKE", name = "WAKE", command = "WOL", color = MaterialTheme.colorScheme.primary.toArgb(), iconName = "PowerSettingsNew"),
        PcButton(id = "LOCK", name = "LOCK", command = if (profile.platform == Platform.WINDOWS) {
            "cmd /c \"schtasks /create /tn LockPC /tr \"rundll32.exe user32.dll,LockWorkStation\" /sc once /st 23:59 /ru INTERACTIVE /f & schtasks /run /tn LockPC & schtasks /delete /tn LockPC /f\""
        } else "loginctl lock-session || xdg-screensaver lock", color = Color(0xFF00D2FF).toArgb(), iconName = "Lock"),
        PcButton(id = "RESTART", name = "RESTART", command = if (profile.platform == Platform.WINDOWS) "shutdown /r /f /t 0" else "sudo reboot", color = Color(0xFFFFB74D).toArgb(), iconName = "Refresh", requireConfirmation = true),
        PcButton(id = "SHUTDOWN", name = "SHUTDOWN", command = if (profile.platform == Platform.WINDOWS) "shutdown /s /f /t 0" else "sudo shutdown -h now", color = Color(0xFFFF4B4B).toArgb(), iconName = "PowerOff", requireConfirmation = true)
    )

    val currentButtons = if (profile.customButtons.isEmpty()) defaultButtons else profile.customButtons

    if (showConfirmDialog != null) {
        val btn = showConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            title = { Text("Confirm Action") },
            text = { Text("Are you sure you want to trigger '${btn.name}' on this device?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            if (btn.command == "WOL") {
                                onResult(sendWakeOnLan(profile.mac, profile.ip, profile.wolPort))
                            } else {
                                val finalCmd = if (btn.runAsAdmin) "ADMIN:${btn.command}" else btn.command
                                onResult(runSshCommand(profile, finalCmd))
                            }
                        }
                        showConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = null }) { Text("Cancel") } }
        )
    }

    if (showEditButtonDialog != null || showAddButtonDialog) {
        val editing = showEditButtonDialog
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var command by remember { mutableStateOf(editing?.command ?: "") }
        var colorArgb by remember { mutableStateOf(editing?.color ?: Color.Gray.toArgb()) }
        var requireConf by remember { mutableStateOf(editing?.requireConfirmation ?: false) }
        var runAsAdmin by remember { mutableStateOf(editing?.runAsAdmin ?: false) }
        
        AlertDialog(
            onDismissRequest = { 
                showEditButtonDialog = null
                showAddButtonDialog = false 
            },
            title = { Text(if (editing != null) "Edit Button" else "Add Custom Button") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name, 
                        onValueChange = { name = it }, 
                        label = { Text("Button Name") }, 
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                    )
                    OutlinedTextField(
                        value = command, 
                        onValueChange = { command = it }, 
                        label = { Text("SSH Command (or 'WOL')") }, 
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = requireConf, onCheckedChange = { requireConf = it })
                        Text("Require confirmation")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = runAsAdmin, onCheckedChange = { runAsAdmin = it })
                        Text("Run as Administrator (Windows)")
                    }
                    Text("Choose Color", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Color(0xFF00F5A0), Color(0xFF00D2FF), Color(0xFFFF5252), Color(0xFFFFB74D), Color(0xFFBB86FC)).forEach { c ->
                            Box(
                                Modifier.size(32.dp).clip(CircleShape).background(c).border(if (colorArgb == c.toArgb()) 2.dp else 0.dp, Color.White, CircleShape).clickable { colorArgb = c.toArgb() }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newBtn = PcButton(
                        id = editing?.id ?: java.util.UUID.randomUUID().toString(), 
                        name = name, 
                        command = command, 
                        color = colorArgb,
                        requireConfirmation = requireConf,
                        runAsAdmin = runAsAdmin
                    )
                    val newList = if (editing != null) {
                        currentButtons.map { if (it.id == editing.id) newBtn else it }
                    } else {
                        currentButtons + newBtn
                    }
                    onUpdateProfile(profile.copy(customButtons = newList))
                    showEditButtonDialog = null
                    showAddButtonDialog = false
                }) { Text("Save") }
            },
            dismissButton = { 
                if (editing != null) {
                    TextButton(onClick = {
                        onUpdateProfile(profile.copy(customButtons = currentButtons.filter { it.id != editing.id }))
                        showEditButtonDialog = null
                    }) { Text("Delete", color = Color.Red) }
                }
                TextButton(onClick = { 
                    showEditButtonDialog = null
                    showAddButtonDialog = false 
                }) { Text("Cancel") } 
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile.name, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { showAddButtonDialog = true }) {
                        Icon(Icons.Default.AddCircleOutline, "Add Button")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        var draggedId by remember { mutableStateOf<String?>(null) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }
        val itemPositions = remember { mutableStateMapOf<String, Offset>() }
        var listVersion by remember { mutableStateOf(0) }

        key(listVersion) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { editingButtonId = null }
                    },
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(2) }) {
                    val statusColor = if (status == DeviceStatus.ONLINE) Color(0xFF00FF9D) else Color(0xFFFF4B4B)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(statusColor.copy(alpha = 0.1f), CircleShape)
                                    .border(1.dp, statusColor.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (status == DeviceStatus.ONLINE) Icons.Default.Computer else Icons.Default.DesktopAccessDisabled,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = statusColor
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(fetchedHostname ?: profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(status.name, style = MaterialTheme.typography.labelMedium, color = statusColor, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    Text("User: ${profile.user}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(modifier = Modifier.padding(top = 2.dp)) {
                                    Text("IP: ${profile.ip}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Row(modifier = Modifier.padding(top = 2.dp)) {
                                    Text("MAC: ${profile.mac}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                itemsIndexed(currentButtons, key = { _, btn -> btn.id }) { index, btn ->
                    val isDragging = draggedId == btn.id
                    val currentBtnId = btn.id
                    val isEditingMe = editingButtonId == currentBtnId
                    
                    ActionButton(
                        name = btn.name,
                        icon = when(btn.iconName) {
                            "PowerSettingsNew" -> Icons.Default.PowerSettingsNew
                            "Lock" -> Icons.Default.Lock
                            "Bedtime" -> Icons.Default.Bedtime
                            "Refresh" -> Icons.Default.Refresh
                            "PowerOff" -> Icons.Default.PowerOff
                            else -> Icons.Default.Terminal
                        },
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                itemPositions[currentBtnId] = coords.positionInParent()
                            }
                            .zIndex(if (isDragging) 10f else 1f)
                            .offset {
                                if (isDragging) {
                                    IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                } else IntOffset.Zero
                            }
                            .pointerInput(currentButtons, editingButtonId) {
                                detectTapGestures(
                                    onLongPress = { editingButtonId = currentBtnId },
                                    onTap = {
                                        if (isEditingMe) {
                                            editingButtonId = null
                                        } else {
                                            if (btn.command == "TERMINAL") {
                                                onTerminalClick(profile.id)
                                            } else if (btn.requireConfirmation || btn.runAsAdmin) {
                                                showConfirmDialog = btn
                                            } else {
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    if (btn.command == "WOL") {
                                                        onResult(sendWakeOnLan(profile.mac, profile.ip, profile.wolPort))
                                                    } else {
                                                        onResult(runSshCommand(profile, btn.command, true))
                                                    }
                                                }
                                            }
                                            editingButtonId = null
                                        }
                                    }
                                )
                            }
                            .pointerInput(currentButtons, editingButtonId) {
                                if (isEditingMe) {
                                    detectDragGestures(
                                        onDragStart = { 
                                            draggedId = currentBtnId
                                            dragOffset = Offset.Zero
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount
                                            
                                            val currentPos = (itemPositions[currentBtnId] ?: Offset.Zero) + dragOffset
                                            
                                            for (i in 0 until currentButtons.size) {
                                                val otherBtn = currentButtons[i]
                                                if (otherBtn.id == currentBtnId) continue
                                                val otherPos = itemPositions[otherBtn.id] ?: continue
                                                
                                                if (abs(currentPos.x - otherPos.x) < 80 &&
                                                    abs(currentPos.y - otherPos.y) < 80) {
                                                    
                                                    val newList = currentButtons.toMutableList()
                                                    val fromIndex = newList.indexOfFirst { it.id == currentBtnId }
                                                    val toIndex = i
                                                    
                                                    if (fromIndex != -1) {
                                                        val item = newList.removeAt(fromIndex)
                                                        newList.add(toIndex, item)
                                                        
                                                        dragOffset = currentPos - otherPos
                                                        listVersion++
                                                        onUpdateProfile(profile.copy(customButtons = newList))
                                                    }
                                                    break
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggedId = null
                                            dragOffset = Offset.Zero
                                        },
                                        onDragCancel = {
                                            draggedId = null
                                            dragOffset = Offset.Zero
                                        }
                                    )
                                }
                            },
                        color = Color(btn.color),
                        showEditIcon = isEditingMe && !isDragging,
                        onEdit = { 
                            showEditButtonDialog = btn
                            editingButtonId = null
                        }
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onTerminalClick(profile.id) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Terminal, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("SSH TERMINAL", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = { showCustomCommandDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Code, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("QUICK COMMAND", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }

    if (showCustomCommandDialog) {
        var customCmd by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomCommandDialog = false },
            title = { Text("Send SSH Command") },
            text = {
                OutlinedTextField(
                    value = customCmd,
                    onValueChange = { customCmd = it },
                    label = { Text("Command") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        onResult(runSshCommand(profile, customCmd))
                    }
                    showCustomCommandDialog = false
                }) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { showCustomCommandDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TerminalScreen(
    profile: PcProfile,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var logs by remember { mutableStateOf(listOf("Connecting to ${profile.user}@${profile.ip}...")) }
    var currentCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        listState.animateScrollToItem(if (logs.isNotEmpty()) logs.size - 1 else 0)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TERMINAL - ${profile.name}", color = Color.Green, style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.Green)
                }
            }
            
            HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        color = if (log.startsWith("Error:") || log.startsWith("SSH Error:")) Color.Red else Color.LightGray,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("> ", color = Color.Green, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = currentCommand,
                    onValueChange = { currentCommand = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Green,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (currentCommand.isNotBlank()) {
                            val cmd = currentCommand
                            logs = logs + "$ $cmd"
                            currentCommand = ""
                            coroutineScope.launch(Dispatchers.IO) {
                                val output = runSshCommand(profile, cmd)
                                withContext(Dispatchers.Main) {
                                    logs = logs + output
                                }
                            }
                        }
                    })
                )
            }
        }
    }
}
