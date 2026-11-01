package ai.aksuper.agent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val OwnerInk = Color(0xFF0C0A08)
private val ErrorRed = Color(0xFFB3261E)

@Composable
fun OwnerScreen(onBack: () -> Unit, onServerCheck: () -> Unit) {
    val context = LocalContext.current
    var setUp by remember { mutableStateOf(OwnerAuth.isSetUp(context)) }
    var loggedIn by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹ Back", color = OwnerInk) }
        when {
            loggedIn -> OwnerDashboard(onLogout = { loggedIn = false }, onServerCheck = onServerCheck)
            !setUp -> OwnerSetup(onDone = { setUp = true; loggedIn = true })
            else -> OwnerLogin(onSuccess = { loggedIn = true })
        }
    }
}

@Composable
private fun OwnerSetup(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Text("Set owner password", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        "First-time setup on this phone. Choose a new password that you do not use anywhere else. " +
            "It is never stored in the app — only a secure hash is kept on this phone.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = OwnerAuth.OWNER_EMAIL, onValueChange = {}, readOnly = true,
        label = { Text("Owner email") }, singleLine = true, modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    PasswordField("New password (min ${OwnerAuth.MIN_LENGTH} characters)", pass) { pass = it }
    Spacer(Modifier.height(8.dp))
    PasswordField("Confirm password", confirm) { confirm = it }
    if (error.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(error, color = ErrorRed)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        enabled = !busy,
        onClick = {
            error = when {
                pass.length < OwnerAuth.MIN_LENGTH -> "Password must be at least ${OwnerAuth.MIN_LENGTH} characters."
                pass != confirm -> "Passwords do not match."
                else -> ""
            }
            if (error.isEmpty()) {
                busy = true
                scope.launch {
                    withContext(Dispatchers.Default) { OwnerAuth.setPassword(context, pass) }
                    pass = ""; confirm = ""; busy = false
                    onDone()
                }
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = OwnerInk),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) { Text(if (busy) "Saving…" else "Save and open dashboard") }
}

@Composable
private fun OwnerLogin(onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Text("Owner / Admin login", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    PasswordField("Password", pass) { pass = it }
    if (message.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(message, color = ErrorRed)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        enabled = !busy && email.isNotBlank() && pass.isNotEmpty(),
        onClick = {
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.Default) { OwnerAuth.verify(context, email, pass) }
                busy = false
                when (result) {
                    is OwnerAuth.Result.Ok -> { pass = ""; message = ""; onSuccess() }
                    is OwnerAuth.Result.Wrong -> message = "Wrong email or password. ${result.attemptsLeft} attempts left."
                    is OwnerAuth.Result.Locked -> message = "Too many attempts. Try again in ${(result.secondsLeft + 59) / 60} min."
                }
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = OwnerInk),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) { Text(if (busy) "Checking…" else "Login") }
    Spacer(Modifier.height(16.dp))
    Text(
        "Forgot password? In this version it can only be reset by clearing the app's data " +
            "(Settings → Apps → AK Super Agent → Storage → Clear data). Email reset arrives with the server in Stage 3.",
        style = MaterialTheme.typography.bodySmall, color = Color.Gray
    )
}

@Composable
private fun OwnerDashboard(onLogout: () -> Unit, onServerCheck: () -> Unit) {
    Text("Owner Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(OwnerAuth.OWNER_EMAIL, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    Spacer(Modifier.height(16.dp))

    val modules = listOf(
        Triple("Online AI chat", "Not connected", "Needs server + AI API key · Stage 5"),
        Triple("Offline AI model", "Not installed", "Model download + llama.cpp · Stage 6"),
        Triple("Projects & memory", "Not built", "Stage 7"),
        Triple("Coding sandbox", "Not built", "Needs server · Stage 8"),
        Triple("Agents", "Not built", "Stage 9"),
        Triple("Breach & threat alerts", "Not built", "Legal breach-notification sources only · Stage 11"),
        Triple("24/7 website monitoring", "Not built", "Needs server · Stage 12")
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            modules.forEach { (name, status, note) ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(status, color = Color.Gray)
                    }
                    Text(note, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = onServerCheck, modifier = Modifier.fillMaxWidth()) {
        Text("Server & device check", color = OwnerInk)
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onLogout,
        colors = ButtonDefaults.buttonColors(containerColor = OwnerInk),
        modifier = Modifier.fillMaxWidth()
    ) { Text("Logout") }
}

@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
}
