package com.example.voicetranscriber

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class MainActivity : ComponentActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioFile = File(cacheDir, "recording.m4a")

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TranscriberScreen(
                        onStartRecording = { startRecording() },
                        onStopRecording = { stopRecording() },
                        audioFile = audioFile
                    )
                }
            }
        }
    }

    private fun startRecording() {
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(64000)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to initialize recorder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaRecorder = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriberScreen(
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    audioFile: File?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableStateOf(0L) }
    var transcriptionText by remember { mutableStateOf("No transcriptions yet. Record something above to get started.") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // API Key State
    var apiKey by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                isRecording = true
                onStartRecording()
            } else {
                Toast.makeText(context, "Microphone permission required!", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSec = 0L
            while (isRecording) {
                delay(1000L)
                recordingDurationSec++
            }
        }
    }

    val formattedTime = remember(recordingDurationSec) {
        val minutes = (recordingDurationSec / 60).toString().padStart(2, '0')
        val seconds = (recordingDurationSec % 60).toString().padStart(2, '0')
        "$minutes:$seconds"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Recorder & Transcriber", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Replaced Popup Dialog with a Direct Input Field
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("API Key Configuration", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Paste Gemini API Key Here") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (apiKey.isEmpty()) "⚠️ Required to transcribe audio." else "✅ API Key configured.",
                        fontSize = 12.sp,
                        color = if (apiKey.isEmpty()) Color.Red else Color.DarkGray
                    )
                }
            }

            Text(
                text = if (isRecording) "Recording... $formattedTime" else "Tap to Record",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                IconButton(
                    onClick = {
                        if (isRecording) {
                            isRecording = false
                            onStopRecording()
                            if (apiKey.isEmpty()) {
                                errorMessage = "Please paste your Gemini API Key in the box above first!"
                            } else {
                                scope.launch {
                                    transcribeAudio(context, audioFile, apiKey, { isLoading = it }, { errorMessage = it }, { transcriptionText = it })
                                }
                            }
                        } else {
                            val recordPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                            if (recordPermission == PackageManager.PERMISSION_GRANTED) {
                                isRecording = true
                                onStartRecording()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier.size(110.dp).clip(CircleShape).background(if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = "Action Button", tint = Color.White, modifier = Modifier.size(54.dp))
                }
            }

            if (isLoading) {
                CircularProgressIndicator()
                Text("Analyzing your audio...", fontSize = 14.sp)
            }

            errorMessage?.let { error ->
                Text(text = "Error: $error", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Transcription", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(transcriptionText))
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            },
                            enabled = transcriptionText.isNotEmpty() && !isLoading
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy text")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                        Text(text = transcriptionText, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.SansSerif, lineHeight = 22.sp)
                    }
                }
            }
        }
    }
}

private suspend fun transcribeAudio(
    context: Context,
    audioFile: File?,
    apiKey: String,
    onLoading: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onSuccess: (String) -> Unit
) {
    if (audioFile == null || !audioFile.exists()) {
        onError("No valid audio file recorded yet.")
        return
    }

    onLoading(true)
    onError(null)

    withContext(Dispatchers.IO) {
        try {
            val audioBytes = FileInputStream(audioFile).use { it.readBytes() }
            val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = apiKey)
            val prompt = content {
                blob("audio/mp4", audioBytes)
                text("Please provide a highly accurate, verbatim transcription of this audio.")
            }
            val response = model.generateContent(prompt)
            val responseText = response.text
            withContext(Dispatchers.Main) {
                if (responseText != null) onSuccess(responseText) else onError("Could not extract transcript.")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError("Failed: ${e.localizedMessage}") }
        } finally {
            withContext(Dispatchers.Main) { onLoading(false) }
        }
    }
}
