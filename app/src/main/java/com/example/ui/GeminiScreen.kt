package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.model.ChatMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiScreen(viewModel: GeminiViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini AI", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setApiKeyDialogOpen(true) }) {
                        Icon(Icons.Outlined.Key, contentDescription = "API Key", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear Chat")
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, modifier = Modifier.imePadding().navigationBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = viewModel::onInputTextChanged,
                        placeholder = { Text("اسأل Gemini أي شيء...") },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { viewModel.sendMessage() },
                        enabled = uiState.inputText.isNotBlank() && !uiState.isLoading,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Quick Chips Row
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.quickPrompts.forEach { prompt ->
                    AssistChip(
                        onClick = { viewModel.sendMessage(prompt) },
                        label = { Text(prompt, maxLines = 1) },
                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }

            // Chat Messages
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.messages, key = { it.id }) { msg ->
                    val isUser = msg.isUser
                    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = bubbleColor, contentColor = textColor),
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(msg.text, fontSize = 15.sp, lineHeight = 22.sp)
                                if (!isUser) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Gemini", msg.text))
                                            coroutineScope.launch { snackbarHostState.showSnackbar("تم النسخ!") }
                                        },
                                        modifier = Modifier.size(24.dp).align(Alignment.End)
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showApiKeyDialog) {
        var tempKey by remember { mutableStateOf(uiState.customApiKey) }
        AlertDialog(
            onDismissRequest = { viewModel.setApiKeyDialogOpen(false) },
            title = { Text("إعداد مفتاح Gemini API") },
            text = {
                OutlinedTextField(
                    value = tempKey,
                    onValueChange = { tempKey = it },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveCustomApiKey(tempKey) }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setApiKeyDialogOpen(false) }) { Text("إلغاء") }
            }
        )
    }
}
