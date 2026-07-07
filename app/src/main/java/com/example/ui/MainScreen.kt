package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.ui.components.AiAssistantBottomSheet
import com.example.ui.components.CodeEditor
import com.example.ui.components.CodePreviewer
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryBronze
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.Typography
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: EditorViewModel) {
    val files by viewModel.files.collectAsState()
    val currentFile by viewModel.currentFile.collectAsState()
    val fileContent by viewModel.fileContent.collectAsState()
    val isPreviewMode by viewModel.isPreviewMode.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAiSheet by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCreatorInfo by remember { mutableStateOf(false) }
    var targetFolderForNew by remember { mutableStateOf<com.example.models.FileModel?>(null) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importFile(it) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurface,
                drawerShape = RectangleShape,
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("FILES", style = Typography.labelLarge, color = PrimaryPurple)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn {
                        items(files) { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (file.isDirectory) {
                                            // Handle folder click if needed, currently just empty
                                        } else {
                                            viewModel.openFile(file)
                                            scope.launch { drawerState.close() }
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width((file.level * 16).dp))
                                Icon(
                                    imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (file.isDirectory) SecondaryBronze else TextSecondary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = file.name,
                                    style = Typography.bodyMedium,
                                    color = if (currentFile?.path == file.path) PrimaryPurple else TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                    if (file.isDirectory) {
                                        IconButton(onClick = { 
                                            targetFolderForNew = file
                                            showNewFileDialog = true 
                                        }) {
                                            Icon(Icons.Default.Add, contentDescription = "Tambah File", tint = TextSecondary)
                                        }
                                        IconButton(onClick = { 
                                            targetFolderForNew = file
                                            showNewFolderDialog = true 
                                        }) {
                                            Icon(Icons.Default.CreateNewFolder, contentDescription = "Tambah Folder", tint = TextSecondary)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteFile(file) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = TextSecondary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { 
                                targetFolderForNew = null
                                showNewFileDialog = true 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple, contentColor = DarkBackground),
                            shape = RectangleShape,
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "File Baru")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("File", maxLines = 1, style = Typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                targetFolderForNew = null
                                showNewFolderDialog = true 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryBronze, contentColor = DarkBackground),
                            shape = RectangleShape,
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Folder Baru")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Folder", maxLines = 1, style = Typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { importLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = TextPrimary),
                            shape = RectangleShape,
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Import")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import", maxLines = 1, style = Typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                val path = viewModel.exportCurrentFile()
                                if (path != null) {
                                    exportMessage = "Diekspor ke: $path"
                                } else {
                                    exportMessage = "Buka file untuk mengekspor"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = TextPrimary),
                            shape = RectangleShape,
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Export")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", maxLines = 1, style = Typography.bodySmall)
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentFile?.name ?: "No File Selected",
                            style = Typography.bodyMedium,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.togglePreview() }) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Preview",
                                tint = if (isPreviewMode) PrimaryPurple else TextPrimary
                            )
                        }
                        IconButton(onClick = { 
                            if (viewModel.settings.isConfigured()) {
                                showAiSheet = true 
                            } else {
                                showSettingsDialog = true
                            }
                        }) {
                            Icon(Icons.Default.Star, contentDescription = "AI Assistant", tint = AccentRed)
                        }
                        IconButton(onClick = { showCreatorInfo = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Creator Info", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBackground
                    )
                )
            },
            containerColor = DarkBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (currentFile != null) {
                    if (isPreviewMode) {
                        CodePreviewer(
                            code = fileContent.text,
                            fileName = currentFile!!.name
                        )
                    } else {
                        CodeEditor(
                            value = fileContent,
                            onValueChange = { viewModel.updateContent(it) },
                            fileName = currentFile!!.name
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Buka file dari menu", color = TextSecondary)
                    }
                }
            }
        }
    }

    if (showAiSheet) {
        AiAssistantBottomSheet(
            onDismiss = { showAiSheet = false },
            aiResponse = aiResponse,
            isLoading = isAiLoading,
            onSendPrompt = { prompt -> viewModel.askAi(prompt) }
        )
    }

    if (showSettingsDialog) {
        var endpoint by remember { mutableStateOf(viewModel.settings.endpoint) }
        var apiKey by remember { mutableStateOf(viewModel.settings.apiKey) }
        var modelName by remember { mutableStateOf(viewModel.settings.modelName) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = DarkSurface,
            shape = RectangleShape,
            title = { Text("Konfigurasi AI (OpenAI Compatible)", color = AccentRed, style = Typography.titleLarge) },
            text = {
                Column {
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it },
                        label = { Text("Endpoint URL", color = TextSecondary) },
                        textStyle = Typography.bodyMedium.copy(color = TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentRed,
                            unfocusedBorderColor = TextSecondary,
                            cursorColor = AccentRed
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key", color = TextSecondary) },
                        textStyle = Typography.bodyMedium.copy(color = TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentRed,
                            unfocusedBorderColor = TextSecondary,
                            cursorColor = AccentRed
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Nama Model", color = TextSecondary) },
                        textStyle = Typography.bodyMedium.copy(color = TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentRed,
                            unfocusedBorderColor = TextSecondary,
                            cursorColor = AccentRed
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.settings.endpoint = endpoint
                        viewModel.settings.apiKey = apiKey
                        viewModel.settings.modelName = modelName
                        showSettingsDialog = false
                        showAiSheet = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RectangleShape
                ) {
                    Text("Simpan", color = DarkBackground)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                    shape = RectangleShape
                ) {
                    Text("Batal", color = TextPrimary)
                }
            }
        )
    }

    if (showCreatorInfo) {
        AlertDialog(
            onDismissRequest = { showCreatorInfo = false },
            containerColor = DarkSurface,
            shape = RectangleShape,
            title = { Text("Informasi Pembuat", color = AccentRed, style = Typography.titleLarge) },
            text = {
                Column {
                    Text("Zhaw (Shadiq)", color = TextPrimary, style = Typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tiktok", color = PrimaryPurple, modifier = Modifier.clickable { uriHandler.openUri("https://www.tiktok.com/@ravmoise?_r=1&_t=ZS-97pa7mwQZzk") }.padding(vertical = 4.dp))
                    Text("Instagram", color = PrimaryPurple, modifier = Modifier.clickable { uriHandler.openUri("https://www.instagram.com/mas_ukkantext?igsh=MXNqeW4xYzg0NGR0aA==") }.padding(vertical = 4.dp))
                    Text("Github", color = PrimaryPurple, modifier = Modifier.clickable { uriHandler.openUri("https://github.com/shawawah12-alt") }.padding(vertical = 4.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCreatorInfo = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RectangleShape
                ) {
                    Text("Tutup", color = DarkBackground)
                }
            }
        )
    }

    if (exportMessage != null) {
        AlertDialog(
            onDismissRequest = { exportMessage = null },
            containerColor = DarkSurface,
            shape = RectangleShape,
            title = { Text("Export Berhasil", color = AccentRed) },
            text = { Text(exportMessage ?: "", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = { exportMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RectangleShape
                ) {
                    Text("Tutup", color = DarkBackground)
                }
            }
        )
    }

    if (showNewFileDialog) {
        var newFileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            containerColor = DarkSurface,
            shape = RectangleShape,
            title = { Text(if (targetFolderForNew != null) "File di ${targetFolderForNew!!.name}" else "Buat File Baru", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    placeholder = { Text("index.js", color = TextSecondary) },
                    textStyle = Typography.bodyMedium.copy(color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = TextSecondary,
                        cursorColor = PrimaryPurple
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createFile(newFileName, targetFolderForNew)
                        }
                        showNewFileDialog = false
                        targetFolderForNew = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RectangleShape
                ) {
                    Text("Buat", color = DarkBackground)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showNewFileDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                    shape = RectangleShape
                ) {
                    Text("Batal", color = TextPrimary)
                }
            }
        )
    }

    if (showNewFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = DarkSurface,
            shape = RectangleShape,
            title = { Text(if (targetFolderForNew != null) "Folder di ${targetFolderForNew!!.name}" else "Buat Folder Baru", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("Nama folder", color = TextSecondary) },
                    textStyle = Typography.bodyMedium.copy(color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryBronze,
                        unfocusedBorderColor = TextSecondary,
                        cursorColor = SecondaryBronze
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName, targetFolderForNew)
                        }
                        showNewFolderDialog = false
                        targetFolderForNew = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryBronze),
                    shape = RectangleShape
                ) {
                    Text("Buat", color = DarkBackground)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showNewFolderDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                    shape = RectangleShape
                ) {
                    Text("Batal", color = TextPrimary)
                }
            }
        )
    }
}
