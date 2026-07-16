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
    var transcriptionText by remember { mutableStateOf("No transcriptions yet. Record something to start.") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var apiKey by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(true) }

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

    Scaffold(topBar = { TopAppBar(title = { Text("AI Voice Transcriber") }) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { showApiKeyDialog = true }) { Text("Configure API Key") }
            
            IconButton(
                onClick = {
                    if (isRecording) {
                        isRecording = false
                        onStopRecording()
                        scope.launch { transcribeAudio(context, audioFile, apiKey, { isLoading = it }, { errorMessage = it }, { transcriptionText = it }) }
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            isRecording = true
                            onStartRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier.size(100.dp).clip(CircleShape).background(if (isRecording) Color.Red else Color.Blue)
            ) {
                Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null, tint = Color.White)
            }
            
            if (isLoading) CircularProgressIndicator()
            Text(text = transcriptionText, modifier = Modifier.verticalScroll(rememberScrollState()))
        }
    }
    
    if (showApiKeyDialog) {
        AlertDialog(onDismissRequest = { showApiKeyDialog = false }, title = { Text("API Key") }, text = { /* Dialog content */ }, confirmButton = { Button(onClick = { showApiKeyDialog = false }) { Text("Save") } })
    }
}

private suspend fun transcribeAudio(context: Context, audioFile: File?, apiKey: String, onLoading: (Boolean) -> Unit, onError: (String?) -> Unit, onSuccess: (String) -> Unit) {
    if (audioFile == null || !audioFile.exists()) { onError("No audio file found."); return }
    onLoading(true)
    withContext(Dispatchers.IO) {
        try {
            val audioBytes = FileInputStream(audioFile).use { it.readBytes() }
            val model = GenerativeModel("gemini-1.5-flash", apiKey)
            val response = model.generateContent(
                content {
                    blob("audio/mp4", audioBytes)
                    text("Transcribe this audio.")
                }
            )
            withContext(Dispatchers.Main) { response.text?.let { onSuccess(it) } ?: onError("No text returned") }
        } catch (e: Exception) { withContext(Dispatchers.Main) { onError(e.localizedMessage) } }
        finally { withContext(Dispatchers.Main) { onLoading(false) } }
    }
}
