package ai.antigem.data.jarvis

import android.content.Context
import android.content.SharedPreferences
import ai.antigem.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Jarvis's specialist agents: persistent personas (lawyer, accountant, geologist, …) that Jarvis recruits and consults.
 * Each has its own knowledge-base folder under the app's data dir. Stored locally (SharedPreferences + files).
 */
object Specialists {

    data class Specialist(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val role: String,
        val personaPrompt: String,
        val tools: List<String> = emptyList(),
        /** Relative folder name under specialists/ for this agent's knowledge base. */
        val knowledgeBase: String = "",
        val createdAt: Long = System.currentTimeMillis()
    )

    private var sp: SharedPreferences? = null
    private var filesDir: String = "/data/data/${BuildConfig.APPLICATION_ID}/files"

    private val _list = MutableStateFlow<List<Specialist>>(emptyList())
    val list: StateFlow<List<Specialist>> = _list.asStateFlow()

    fun init(context: Context) {
        if (sp != null) return
        val p = context.applicationContext.getSharedPreferences("oshen_specialists", Context.MODE_PRIVATE)
        filesDir = context.applicationContext.filesDir.absolutePath
        _list.value = runCatching {
            val arr = JSONArray(p.getString("list", "[]"))
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Specialist(
                    id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = o.optString("name"),
                    role = o.optString("role"),
                    personaPrompt = o.optString("personaPrompt"),
                    tools = o.optJSONArray("tools")?.let { t -> (0 until t.length()).map { t.getString(it) } } ?: emptyList(),
                    knowledgeBase = o.optString("knowledgeBase"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            }
        }.getOrDefault(emptyList())
        sp = p
        // seed defaults if empty
        if (_list.value.isEmpty()) seedDefaults(context)
    }

    private fun save() {
        val p = sp ?: return
        val arr = JSONArray()
        _list.value.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id); put("name", s.name); put("role", s.role)
                put("personaPrompt", s.personaPrompt)
                put("tools", JSONArray(s.tools))
                put("knowledgeBase", s.knowledgeBase); put("createdAt", s.createdAt)
            })
        }
        p.edit().putString("list", arr.toString()).apply()
    }

    fun add(s: Specialist) {
        val kb = s.knowledgeBase.ifBlank { s.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-') }
        val withKb = s.copy(knowledgeBase = kb)
        val dir = File(filesDir, "specialists/$kb")
        dir.mkdirs()
        _list.value = _list.value + withKb
        save()
    }

    fun remove(id: String) {
        _list.value = _list.value.filter { it.id != id }
        save()
    }

    fun update(s: Specialist) {
        _list.value = _list.value.map { if (it.id == s.id) s else it }
        save()
    }

    fun knowledgeBaseDir(s: Specialist): File = File(filesDir, "specialists/${s.knowledgeBase}")

    /** Seed a generous set of pre-built specialists tailored to the user (Youssef). */
    fun seedDefaults(context: Context) {
        init(context)
        val base = "You are a specialist agent working for Jarvis, serving Youssef Bounakhla. " +
            "Youssef is a 28-year-old geology/GIS master's student at Chang'an University (Xi'an), site lead & ops supervisor " +
            "for mining operations in Morocco (Atlas Ores Morocco, CSMR) across Errachidia, Rich, Bouanane. He speaks Chinese, " +
            "English, Arabic and Darija. Use his personal profile for context. Reply in the configured language."
        val defaults = listOf(
            Specialist(name = "Geo", role = "Geologist & Mining Engineer",
                personaPrompt = "$base You are Geo, an expert geologist and mining engineer. You advise on ore-forming types, Cu-Co deposits (Krema, Bouanane), exploration programs, APC tenders, SEM, XRF, magnetometry, field sampling. Be precise, cite evidence, propose next steps.",
                tools = listOf("web_search", "read_file", "notes")),
            Specialist(name = "Gis", role = "GIS & Remote Sensing Specialist",
                personaPrompt = "$base You are Gis, an expert in ArcGIS Pro, spatial data processing, geological mapping, raster/vector analysis, remote sensing. Give step-by-step ArcGIS workflows.",
                tools = listOf("web_search", "read_file", "notes")),
            Specialist(name = "Xls", role = "Excel & Data Automation Specialist",
                personaPrompt = "$base You are Xls, an expert in Excel, Python (xlsxwriter), VBA macros. Youssef's standards: Times New Roman font, centered & middle-aligned cells, Dark Navy (#0A1F44) + Gold (#D4AF37) headers. Produce ready-to-use workbooks and macros.",
                tools = listOf("read_file", "notes")),
            Specialist(name = "Tender", role = "Procurement & Tender Specialist",
                personaPrompt = "$base You are Tender, an expert in Moroccan mining permits, bidding dossiers, 3-year geological exploration work programs, APC tenders (e.g. APC 01/DGMH/2026). Draft compliant dossiers.",
                tools = listOf("web_search", "read_file", "notes")),
            Specialist(name = "Power", role = "Electrical & Power Engineer",
                personaPrompt = "$base You are Power, an expert in electrical capacity upgrades (630/800 kVA), SRM/ONEE coordination, generator propane supply (Vitogaz), factory power. Advise on sizing, coordination, safety.",
                tools = listOf("web_search", "read_file", "notes")),
            Specialist(name = "Lawyer", role = "Moroccan Lawyer",
                personaPrompt = "$base You are Lawyer, a Moroccan legal advisor. You advise on mining law, customs coordination, contracts, labor law (Moroccan). Always flag that you are not a substitute for a licensed attorney.",
                tools = listOf("web_search", "read_file", "notes")),
            Specialist(name = "Accountant", role = "Accountant & Finance",
                personaPrompt = "$base You are Accountant, a finance specialist for mining operations. You handle deposits, stamping, customs, factory accounting, Atlas Ores Morocco / CSMR books. Be precise with figures.",
                tools = listOf("read_file", "notes", "calc")),
            Specialist(name = "Researcher", role = "Researcher & Thesis Assistant",
                personaPrompt = "$base You are Researcher, an academic assistant for Youssef's master's thesis on the Krema Cu-Co deposit (Bouanane). Help with literature review, writing, citations, formatting.",
                tools = listOf("web_search", "read_file", "notes")),
            Specialist(name = "Zhongwen", role = "Chinese Language Assistant",
                personaPrompt = "$base You are Zhongwen, a Chinese language assistant. You help with academic Chinese, translation, communication with Chinese colleagues (Fany Jun, Wajid Ibrar) and Chang'an University. Reply bilingually when useful.",
                tools = listOf("web_search", "read_file", "notes")),
            Specialist(name = "Pm", role = "Project Manager",
                personaPrompt = "$base You are Pm, a project manager coordinating multi-site operations between Xi'an (study) and Errachidia/Rich/Bouanane (field). Manage schedules, tasks, team coordination.",
                tools = listOf("read_file", "notes")),
            Specialist(name = "Pa", role = "Personal Assistant",
                personaPrompt = "$base You are Pa, Youssef's personal assistant. You know his contacts (Fany Jun, Driss Ait Taleb, Wajid Ibrar, Mr. Aziz, Rachid Oummad, Mr. Ali, his father), schedule, preferences (Les Cafés Dubois espresso, Realme Neo 7). Handle reminders, lookups, daily help.",
                tools = listOf("read_file", "notes"))
        )
        defaults.forEach { add(it) }
    }
}
