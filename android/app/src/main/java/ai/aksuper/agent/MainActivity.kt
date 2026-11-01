package ai.aksuper.agent

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val LogoBg = Color(0xFFF2F5F7)
private val Ink = Color(0xFF0C0A08)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Ink,
                    onPrimary = Color.White,
                    background = LogoBg,
                    surface = LogoBg,
                    onBackground = Ink,
                    onSurface = Ink
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = LogoBg) {
                    App()
                }
            }
        }
    }
}

enum class Screen { Entry, Public, Owner, Server }

@Composable
fun App() {
    var screen by rememberSaveable { mutableStateOf(Screen.Entry) }
    BackHandler(enabled = screen != Screen.Entry) { screen = Screen.Entry }

    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        when (screen) {
            Screen.Entry -> EntryScreen(onSelect = { screen = it })
            Screen.Public -> ComingSoonScreen(
                title = "Public User",
                body = "Register, email verification, sign-in and Free / Pro / Ultra plans arrive in Stage 3 and 4. " +
                    "They need the live backend, which is not connected yet.",
                onBack = { screen = Screen.Entry }
            )
            Screen.Owner -> ComingSoonScreen(
                title = "Owner / Admin",
                body = "Owner sign-in (email + password + MFA) is created once on the server in Stage 3. " +
                    "No password is stored inside this app.",
                onBack = { screen = Screen.Entry }
            )
            Screen.Server -> ServerScreen(onBack = { screen = Screen.Entry })
        }
    }
}

@Composable
fun EntryScreen(onSelect: (Screen) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(48.dp))
        Image(
            painter = painterResource(id = R.drawable.logo_anika),
            contentDescription = "ANIKA logo",
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(Modifier.height(16.dp))
        Text("AK SUPER AGENT AI", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(
            "One AI. Build, Code, Protect and Automate.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = { onSelect(Screen.Public) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ink)
        ) { Text("PUBLIC USER", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onSelect(Screen.Owner) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("OWNER / ADMIN", fontWeight = FontWeight.Bold, color = Ink) }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = { onSelect(Screen.Server) }) {
            Text("Server & device check", color = Ink)
        }
        Text("v0.2.0 · Stage 2", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
fun ComingSoonScreen(title: String, body: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack) { Text("‹ Back", color = Ink) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ServerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf("https://") }
    var result by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val info = remember { deviceInfo(context) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹ Back", color = Ink) }
        Text("Backend health check", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Server URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            enabled = !busy && url.length > 8,
            onClick = {
                busy = true
                result = "Checking…"
                scope.launch {
                    result = try {
                        checkHealth(url.trim())
                    } catch (e: Exception) {
                        "Failed: ${e.javaClass.simpleName}: ${e.message ?: "no details"}"
                    }
                    busy = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Ink),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Checking…" else "Check /health") }
        if (result.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(result, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("This phone", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                info.forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(k, modifier = Modifier.weight(1f), color = Color.Gray)
                        Text(v, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

suspend fun checkHealth(base: String): String = withContext(Dispatchers.IO) {
    val conn = URL(base.trimEnd('/') + "/health").openConnection() as HttpURLConnection
    conn.connectTimeout = 8000
    conn.readTimeout = 8000
    try {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
        "HTTP $code\n$body"
    } finally {
        conn.disconnect()
    }
}

fun deviceInfo(context: Context): List<Pair<String, String>> {
    val gb = 1024.0 * 1024.0 * 1024.0
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mem = ActivityManager.MemoryInfo()
    am.getMemoryInfo(mem)
    val stat = StatFs(context.filesDir.absolutePath)
    val ramGb = mem.totalMem / gb
    val freeGb = stat.availableBytes / gb
    val offline = when {
        ramGb >= 11 -> "Up to ~7–8B model (Q4)"
        ramGb >= 7 -> "Up to ~3–4B model (Q4)"
        ramGb >= 5 -> "Up to ~1–2B model (Q4)"
        else -> "Online mode recommended"
    }
    return listOf(
        "Device" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "RAM" to String.format("%.1f GB", ramGb),
        "Free storage" to String.format("%.1f GB", freeGb),
        "CPU cores" to Runtime.getRuntime().availableProcessors().toString(),
        "Low-memory now" to if (mem.lowMemory) "Yes" else "No",
        "Offline AI fit" to offline
    )
}
