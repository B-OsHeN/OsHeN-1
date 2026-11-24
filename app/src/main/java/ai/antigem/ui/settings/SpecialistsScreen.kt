package ai.antigem.ui.settings

import ai.antigem.data.jarvis.Specialists
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpecialistsScreen(cardBg: Color, cardBorder: BorderStroke) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) { Specialists.init(context) }
    val list by Specialists.list.collectAsState()
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var persona by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Specialists (${list.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("OsHeN's specialist agents. Each has its own knowledge base under specialists/<name>/.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        if (showAdd) {
            Card(colors = CardDefaults.cardColors(containerColor = cardBg), border = cardBorder) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = persona, onValueChange = { persona = it }, label = { Text("Persona prompt") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (name.isNotBlank() && role.isNotBlank()) {
                                Specialists.add(ai.antigem.data.jarvis.Specialists.Specialist(name = name.trim(), role = role.trim(), personaPrompt = persona.trim()))
                                name = ""; role = ""; persona = ""; showAdd = false
                            }
                        }, modifier = Modifier.weight(1f)) { Text("Add") }
                        OutlinedButton(onClick = { showAdd = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    }
                }
            }
        } else {
            Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) { Text("Recruit a new specialist") }
        }
        list.forEach { sp ->
            Card(colors = CardDefaults.cardColors(containerColor = cardBg), border = cardBorder) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sp.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(sp.role, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(sp.personaPrompt.take(120) + if (sp.personaPrompt.length > 120) "…" else "", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("Knowledge base: specialists/${sp.knowledgeBase}/", fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    TextButton(onClick = { Specialists.remove(sp.id) }) { Text("Remove", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            }
        }
    }
}
