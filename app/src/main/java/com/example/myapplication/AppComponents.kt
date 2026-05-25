package com.example.myapplication

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableDeviceCard(
    profile: PcProfile,
    status: DeviceStatus,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onWake: () -> Unit,
    onShutdown: () -> Unit,
    showEditIcon: Boolean = false,
    isSwipable: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val anchorSize = with(density) { 80.dp.toPx() }
    
    val state = remember {
        AnchoredDraggableState(initialValue = DragValue.Center)
    }

    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        positionalThreshold = { distance: Float -> distance * 0.5f },
        animationSpec = tween<Float>()
    )
    
    LaunchedEffect(anchorSize) {
        state.updateAnchors(
            DraggableAnchors {
                DragValue.Center at 0f
                DragValue.Start at anchorSize
                DragValue.End at -anchorSize
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        val offset = try { state.requireOffset() } catch (e: Exception) { 0f }
        
        if (offset != 0f) {
            val isStart = offset > 0
            val color = if (isStart) MaterialTheme.colorScheme.primary else Color.Red
            val alignment = if (isStart) Alignment.CenterStart else Alignment.CenterEnd
            val icon = if (isStart) Icons.Default.PowerSettingsNew else Icons.Default.PowerOff
            
            Box(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(color)
                    .padding(horizontal = 16.dp),
                contentAlignment = alignment
            ) {
                IconButton(onClick = { 
                    if (isStart) onWake() else onShutdown()
                    coroutineScope.launch { state.animateTo(DragValue.Center) }
                }) {
                    Icon(icon, null, tint = Color.White)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .anchoredDraggable(
                    state = state, 
                    orientation = Orientation.Horizontal,
                    flingBehavior = flingBehavior,
                    enabled = isSwipable
                )
                .fillMaxWidth()
        ) {
            DeviceCard(
                profile = profile, 
                status = status, 
                onClick = onClick, 
                onEdit = onEdit, 
                showEditIcon = showEditIcon
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceCard(
    profile: PcProfile, 
    status: DeviceStatus, 
    onClick: () -> Unit, 
    onEdit: () -> Unit,
    showEditIcon: Boolean = false
) {
    val statusColor = when(status) {
        DeviceStatus.ONLINE -> Color(0xFF00FF9D)
        DeviceStatus.OFFLINE -> Color(0xFFFF4B4B)
        DeviceStatus.CHECKING -> Color.Gray
    }

    val rotation by if (showEditIcon) {
        val infiniteTransition = rememberInfiniteTransition(label = "shake")
        infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(150, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rotation"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(modifier = Modifier.graphicsLayer(rotationZ = rotation)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, CircleShape)
                        .shadow(if (status == DeviceStatus.ONLINE) 6.dp else 0.dp, CircleShape, spotColor = statusColor)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (profile.platform == Platform.WINDOWS) Icons.Default.Window else Icons.Default.Terminal,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.Gray
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(profile.ip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (showEditIcon) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(26.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = Color.Black)
            }
        }
    }
}

@Composable
fun ActionButton(
    name: String, 
    icon: ImageVector, 
    modifier: Modifier = Modifier, 
    color: Color, 
    showEditIcon: Boolean = false,
    onEdit: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val rotation by if (showEditIcon) {
        val infiniteTransition = rememberInfiniteTransition(label = "shake")
        infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(150, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rotation"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(modifier = modifier.graphicsLayer(rotationZ = rotation)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = color.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(name, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black), color = color)
            }
        }
        
        if (showEditIcon) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(26.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    currentAccent: Color,
    onColorChange: (Color) -> Unit,
    currentBg: Color?,
    onBgChange: (Color?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark Theme", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isDarkTheme, onCheckedChange = { onThemeToggle() })
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Accent Color", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val colors = listOf(Color(0xFF00F5A0), Color(0xFF00D2FF), Color(0xFFFF5252), Color(0xFFFFB74D), Color(0xFFBB86FC))
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (currentAccent == color) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            .clickable { onColorChange(color) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Background Color", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))
            
            val bgOptions = if (isDarkTheme) {
                listOf(null, Color(0xFF000000), Color(0xFF1A1A1D), Color(0xFF0B1218))
            } else {
                listOf(null, Color(0xFFFFFFFF), Color(0xFFE3F2FD), Color(0xFFF1F8E9))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bgOptions.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color ?: (if (isDarkTheme) Color(0xFF0B0E11) else Color(0xFFF0F2F5)))
                            .border(if (currentBg == color) 3.dp else 1.dp, Color.Gray, CircleShape)
                            .clickable { onBgChange(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (color == null) Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun ProfileDialog(
    profile: PcProfile?, 
    onDismiss: () -> Unit, 
    onSave: (PcProfile) -> Unit,
    onDelete: ((PcProfile) -> Unit)? = null
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var ip by remember { mutableStateOf(profile?.ip ?: "") }
    var mac by remember { mutableStateOf(profile?.mac ?: "") }
    var user by remember { mutableStateOf(profile?.user ?: "") }
    var pass by remember { mutableStateOf(profile?.pass ?: "") }
    var privateKey by remember { mutableStateOf(profile?.privateKey ?: "") }
    var authModeIsPassword by remember { mutableStateOf(profile?.privateKey == null) }
    var sshPort by remember { mutableStateOf(profile?.sshPort?.toString() ?: "22") }
    var wolPort by remember { mutableStateOf(profile?.wolPort?.toString() ?: "9") }
    var platform by remember { mutableStateOf(profile?.platform ?: Platform.WINDOWS) }
    
    var passVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(if (profile == null) "Connect Device" else "Edit Connection", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (error != null) {
                    item { Text(error!!, color = Color.Red, style = MaterialTheme.typography.labelSmall) }
                }
                item {
                    Column {
                        Text("Platform", style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = platform == Platform.WINDOWS, onClick = { platform = Platform.WINDOWS })
                            Text("Windows", modifier = Modifier.clickable { platform = Platform.WINDOWS })
                            Spacer(Modifier.width(16.dp))
                            RadioButton(selected = platform == Platform.LINUX, onClick = { platform = Platform.LINUX })
                            Text("Linux", modifier = Modifier.clickable { platform = Platform.LINUX })
                        }
                    }
                }
                item { 
                    OutlinedTextField(
                        value = name, 
                        onValueChange = { name = it }, 
                        label = { Text("Alias (e.g. Gaming Rig)") }, 
                        shape = RoundedCornerShape(12.dp), 
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    ) 
                }
                item { 
                    OutlinedTextField(
                        value = ip, 
                        onValueChange = { ip = it }, 
                        label = { Text("IP Address / Hostname") }, 
                        shape = RoundedCornerShape(12.dp), 
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    ) 
                }
                item { 
                    OutlinedTextField(
                        value = mac, 
                        onValueChange = { mac = it }, 
                        label = { Text("MAC Address") }, 
                        shape = RoundedCornerShape(12.dp), 
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    ) 
                }
                item { 
                    OutlinedTextField(
                        value = user, 
                        onValueChange = { user = it }, 
                        label = { Text("SSH Username") }, 
                        shape = RoundedCornerShape(12.dp), 
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    ) 
                }
                item { 
                    Column {
                        Text("Authentication", style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = authModeIsPassword, onClick = { authModeIsPassword = true })
                            Text("Password", modifier = Modifier.clickable { authModeIsPassword = true })
                            Spacer(Modifier.width(16.dp))
                            RadioButton(selected = !authModeIsPassword, onClick = { authModeIsPassword = false })
                            Text("SSH Key", modifier = Modifier.clickable { authModeIsPassword = false })
                        }
                    }
                }
                item { 
                    if (authModeIsPassword) {
                        OutlinedTextField(
                            value = pass, 
                            onValueChange = { pass = it }, 
                            label = { Text("SSH Password") }, 
                            visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passVisible = !passVisible }) {
                                    Icon(if (passVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                                }
                            },
                            shape = RoundedCornerShape(12.dp), 
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    } else {
                        OutlinedTextField(
                            value = privateKey, 
                            onValueChange = { privateKey = it }, 
                            label = { Text("Private Key Content") }, 
                            shape = RoundedCornerShape(12.dp), 
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                        )
                    }
                }
                item { 
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sshPort, 
                            onValueChange = { sshPort = it }, 
                            label = { Text("SSH Port") }, 
                            modifier = Modifier.weight(1f), 
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), 
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = wolPort, 
                            onValueChange = { wolPort = it }, 
                            label = { Text("WOL Port") }, 
                            modifier = Modifier.weight(1f), 
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), 
                            shape = RoundedCornerShape(12.dp),
                            keyboardActions = KeyboardActions(onDone = { 
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            })
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || ip.isBlank() || mac.isBlank() || user.isBlank() || (authModeIsPassword && pass.isBlank()) || (!authModeIsPassword && privateKey.isBlank())) {
                        error = "Please fill all required fields"
                    } else if (mac.replace(":","").replace("-","").length != 12) {
                        error = "Invalid MAC Address"
                    } else {
                        onSave(PcProfile(
                            profile?.id ?: java.util.UUID.randomUUID().toString(),
                            name, ip, mac, user, 
                            if (authModeIsPassword) pass else null,
                            if (authModeIsPassword) null else privateKey,
                            sshPort.toIntOrNull() ?: 22,
                            wolPort.toIntOrNull() ?: 9,
                            platform,
                            profile?.customButtons ?: emptyList(),
                            profile?.hostname
                        ))
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save Configuration") }
        },
        dismissButton = {
            Row {
                if (profile != null && onDelete != null) {
                    TextButton(onClick = { 
                        onDelete(profile)
                        onDismiss()
                    }) {
                        Text("Delete", color = Color.Red)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
