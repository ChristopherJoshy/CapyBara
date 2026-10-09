package dev.capybara.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import dev.capybara.sdk.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Locale

private val Obsidian = Color(0xFF121517)
private val Charcoal = Color(0xFF1D2327)
private val Sand = Color(0xFFF0E5D7)
private val Copper = Color(0xFFD8AE86)
private val Cyan = Color(0xFF98CDC9)
private val Muted = Color(0xFFB3BABD)

class MainActivity : ComponentActivity() {
    private lateinit var sdk: Capybara
    private var captureJob: Job? = null
    private var microphoneGranted by mutableStateOf(false)
    private var busy by mutableStateOf(false)
    private var committing by mutableStateOf(false)
    private var stage by mutableStateOf("Ready")
    private var problem by mutableStateOf<String?>(null)
    private var scan by mutableStateOf<Scan?>(null)
    private var evidence by mutableStateOf<PhysicsEvidence?>(null)
    private var savedCount by mutableIntStateOf(0)
    private var baselines by mutableStateOf<List<ResearchBaseline>>(emptyList())
    private var bundle by mutableStateOf<List<Pair<CaptureLabel, String>>>(emptyList())
    private var savedObjectNames by mutableStateOf<List<String>>(emptyList())
    private var pendingShowcaseScan by mutableStateOf<Scan?>(null)
    private var pendingShowcaseEvidence by mutableStateOf<PhysicsEvidence?>(null)
    private var showcasePaths by mutableStateOf<List<String>>(emptyList())
    private var showcaseFingerprint by mutableStateOf<String?>(null)
    private var logs by mutableStateOf<List<String>>(emptyList())
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        microphoneGranted = granted
        log(if (granted) "Microphone permission granted" else "Microphone permission denied")
        if (!granted) problem = "Enable Capybara's microphone permission to capture."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sdk = Capybara(this)
        logs = savedInstanceState?.getStringArrayList("operationLogs")?.toList() ?: emptyList()
        microphoneGranted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val capabilities = sdk.getCapabilities()
        val restoredPaths = savedInstanceState?.getStringArrayList("bundlePaths") ?: emptyList()
        lifecycleScope.launch {
            try {
                val capturePaths = withContext(Dispatchers.IO) { sdk.listCaptures() }
                savedCount = capturePaths.size
                bundle = withContext(Dispatchers.IO) { restoredPaths.map { sdk.getCaptureLabel(it) to it } }
                savedObjectNames = withContext(Dispatchers.IO) {
                    capturePaths.map { sdk.getCaptureLabel(it) }
                        .filter { it.condition == "showcase_enrollment" }
                        .map { it.objectId }.distinct()
                }
                baselines = withContext(Dispatchers.IO) { SignatureVault(this@MainActivity).listResearchBaselines() }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { problem = "Saved data could not be read."; log("Saved data read failed: ${error.message}") }
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Copper, onPrimary = Obsidian,
                secondary = Cyan, background = Obsidian, onBackground = Sand,
                surface = Charcoal, onSurface = Sand, outline = Muted)) { Instrument(capabilities) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("bundlePaths", ArrayList(bundle.map { it.second }))
        outState.putStringArrayList("operationLogs", ArrayList(logs))
        super.onSaveInstanceState(outState)
    }

    override fun onStop() { captureJob?.cancel(); super.onStop() }

    private fun log(message: String) {
        logs = (logs + "${android.text.format.DateFormat.format("HH:mm:ss", System.currentTimeMillis())}  $message").takeLast(80)
    }

    @Composable
    private fun Instrument(capabilities: DeviceCapabilities) {
        var page by rememberSaveable { mutableStateOf("home") }
        var objectId by rememberSaveable { mutableStateOf("earbuds-case-01") }
        var sessionId by rememberSaveable { mutableStateOf("session-${System.currentTimeMillis()}") }
        var condition by rememberSaveable { mutableStateOf("with_earbuds") }
        var position by rememberSaveable { mutableStateOf("A") }
        var section by rememberSaveable { mutableStateOf("Status") }
        var scientificDetails by rememberSaveable { mutableStateOf(false) }
        var showcaseMode by remember { mutableStateOf<String?>(null) }
        var showcaseName by remember { mutableStateOf("") }
        var showcaseCaptureId by remember { mutableStateOf("") }
        var showcaseSession by remember { mutableStateOf("") }
        var showcaseStep by remember { mutableStateOf("name") }
        var showcaseSide by remember { mutableIntStateOf(1) }
        var showcaseEvidence by remember { mutableStateOf<List<Pair<String, PhysicsEvidence>>>(emptyList()) }
        val availableNames = (baselines.map { it.objectId } + savedObjectNames).distinct()
        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        fun openShowcase(mode: String) {
            showcaseMode = mode
            showcaseName = ""
            showcaseCaptureId = if (mode == "scan") "candidate-${java.util.UUID.randomUUID()}" else ""
            showcaseSession = ""
            showcaseSide = 1
            showcaseStep = if (mode == "scan") "capture" else "name"
            showcaseEvidence = emptyList()
            showcasePaths = emptyList()
            showcaseFingerprint = null
            pendingShowcaseScan = null
            pendingShowcaseEvidence = null
            problem = null
        }

        fun startShowcaseSide() {
            val mode = showcaseMode ?: return
            if (mode == "enroll" && showcaseName.isBlank()) {
                problem = "Enter an object name before capturing."
                return
            }
            if (mode == "enroll") showcaseName = showcaseName.trim()
            val captureObjectId = if (mode == "enroll") showcaseName else showcaseCaptureId
            if (!microphoneGranted) {
                permission.launch(Manifest.permission.RECORD_AUDIO)
                return
            }
            if (showcaseSession.isBlank()) showcaseSession = "showcase-${System.currentTimeMillis()}"
            showcaseStep = "capturing"
            startShowcaseCapture(captureObjectId, showcaseSession, mode, showcaseSide) { succeeded ->
                showcaseStep = if (succeeded) "review" else "capture"
            }
        }

        BackHandler(enabled = drawer.isOpen || page == "capture" || showcaseMode != null) {
            if (drawer.isOpen) scope.launch { drawer.close() }
            else if (!busy) {
                if (showcaseMode != null) {
                    showcaseMode = null
                    pendingShowcaseScan = null
                    pendingShowcaseEvidence = null
                } else page = "home"
            } else if (!committing) captureJob?.cancel()
        }
        ModalNavigationDrawer(drawerState = drawer, gesturesEnabled = false, drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Charcoal) {
                Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Instrument", fontSize = 24.sp, color = Copper)
                        TextButton(onClick = { scope.launch { drawer.close() } }) { Text("Close menu") }
                    }
                    listOf("Status", "Logs", "Capture setup").forEach { title ->
                        NavigationDrawerItem(label = { Text(title) }, selected = section == title, onClick = { section = title })
                    }
                    HorizontalDivider()
                    when (section) {
                        "Status" -> {
                            Text("${capabilities.model} · API ${capabilities.apiLevel}", color = Cyan)
                            Text("Microphone: ${if (microphoneGranted) "permission granted" else "permission needed"}\nCaptures: $savedCount\nBaselines: ${baselines.size}\nCurrent operation: $stage", color = Muted)
                            Text("Intelligence", color = Copper, fontSize = 18.sp)
                            Text("EchoNet, VibraNet, PhysFusion, StateNet, SpatialNet and Laya: checkpoints unavailable.\nCalibration: pending.\nVerification: unavailable.", color = Muted, lineHeight = 21.sp)
                            TextButton(onClick = { scientificDetails = !scientificDetails }) { Text(if (scientificDetails) "Hide technical details" else "Technical details") }
                            if (scientificDetails) {
                                Text(capabilities.audio, color = Muted, fontSize = 12.sp)
                                Text(capabilities.haptics, color = Muted, fontSize = 12.sp)
                                capabilities.sensors.forEach { Text(it, color = Muted, fontSize = 11.sp) }
                                evidence?.limitations?.forEach { Text(it, color = Copper, fontSize = 12.sp) }
                                scan?.metadata?.forEach { (key, value) ->
                                    Text("$key: ${value.take(320)}${if (value.length > 320) "… (full data saved)" else ""}", color = Muted, fontSize = 11.sp)
                                }
                            }
                        }
                        "Logs" -> {
                            if (logs.isEmpty()) Text("No operations in this session.", color = Muted)
                            logs.asReversed().forEach { Text(it, color = Muted, fontSize = 12.sp, lineHeight = 19.sp) }
                        }
                        else -> {
                            OutlinedTextField(objectId, { objectId = it }, label = { Text("Object ID") }, singleLine = true, enabled = !busy)
                            OutlinedTextField(sessionId, { sessionId = it }, label = { Text("Session ID") }, singleLine = true, enabled = !busy)
                            TextButton(onClick = { sessionId = "session-${System.currentTimeMillis()}" }, enabled = !busy) { Text("New session") }
                            Text("Same closed case, table and phone orientation. Mark four stable contact points. Disconnect charging and keep audio on the phone.", color = Muted, lineHeight = 21.sp)
                            Text("Audio and motion data stay private on this phone. These observed-state labels require physical preparation; captures are not model predictions.", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }) {
            Surface(Modifier.fillMaxSize(), color = Obsidian) {
                Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Image(painterResource(R.drawable.ic_capybara), "Capybara", Modifier.size(38.dp))
                            Text("CAPYBARA", letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        IconButton(onClick = { scope.launch { drawer.open() } }, modifier = Modifier.semantics { contentDescription = "Open menu" }) {
                            Canvas(Modifier.size(22.dp)) {
                                for (y in listOf(0.2f, 0.5f, 0.8f)) drawLine(Sand, Offset(0f, size.height * y), Offset(size.width, size.height * y), strokeWidth = 2.dp.toPx())
                            }
                        }
                    }
                    when {
                        page == "capture" -> {
                            TextButton(onClick = { page = "home" }, enabled = !busy, contentPadding = PaddingValues(0.dp)) { Text("Back") }
                            Text("Capture response", fontFamily = FontFamily.Serif, fontSize = 34.sp)
                            Text(objectId, color = Muted)
                            PositionGuide(position)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf("A", "B", "C", "D").forEach { p ->
                                    FilterChip(position == p, { position = p }, enabled = !busy, label = { Text(p) }, modifier = Modifier.weight(1f))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(condition == "with_earbuds", { condition = "with_earbuds" }, enabled = !busy, label = { Text("Earbuds inside") })
                                FilterChip(condition == "without_earbuds", { condition = "without_earbuds" }, enabled = !busy, label = { Text("Empty case") })
                            }
                            if (busy) {
                                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Cyan)
                                Text(if (committing) "Saving baseline" else capturePhase(stage), color = Cyan)
                                if (!committing) OutlinedButton(onClick = { captureJob?.cancel() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                            } else {
                                Button(onClick = {
                                    if (!microphoneGranted) permission.launch(Manifest.permission.RECORD_AUDIO)
                                    else startCapture(objectId, sessionId, condition, position)
                                }, enabled = objectId.isNotBlank() && sessionId.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                    Text(if (microphoneGranted) "Capture $position" else "Enable microphone")
                                }
                            }
                            val matching = bundle.filter { it.first.objectId == objectId.trim() && it.first.sessionId == sessionId.trim() && it.first.condition == condition }
                            if (matching.isNotEmpty()) Text("Saved: ${matching.map { it.first.position }.sorted().joinToString()}", color = Cyan)
                            if (matching.size == 4) OutlinedButton(onClick = { saveBaseline(matching.map { it.second }) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Save baseline") }
                            evidence?.let { measured ->
                                Text("Measured response", color = Copper, fontSize = 20.sp)
                                scan?.let { Waveform(it.pcm) }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Metric("Duration", "${format(measured.durationSeconds)} s")
                                    Metric("Audio RMS", format(measured.audioRms))
                                    Metric("Clipped samples", measured.clippedSamples.toString())
                                }
                            }
                        }
                        showcaseMode != null -> {
                            TextButton(onClick = {
                                if (!busy) {
                                    showcaseMode = null
                                    pendingShowcaseScan = null
                                    pendingShowcaseEvidence = null
                                }
                            }, enabled = !busy, contentPadding = PaddingValues(0.dp)) { Text("Back") }
                            Text(if (showcaseMode == "enroll") "Enroll object" else "Scan object", fontFamily = FontFamily.Serif, fontSize = 34.sp)
                            when (showcaseStep) {
                                "name" -> {
                                    Text("Name the object to label its enrollment captures. The label is not an identity decision.", color = Muted, lineHeight = 21.sp)
                                    OutlinedTextField(
                                        value = showcaseName,
                                        onValueChange = { showcaseName = it },
                                        label = { Text("Object name") },
                                        singleLine = true,
                                        enabled = !busy,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(onClick = { startShowcaseSide() }, enabled = showcaseName.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                        Text(if (microphoneGranted) "Capture Side 1" else "Enable microphone")
                                    }
                                }
                                "capture" -> {
                                    Text(
                                        when {
                                            showcaseMode == "scan" && showcaseSide == 1 -> "Place the phone on any side of the object. After this capture, the app will ask you to scan another side."
                                            showcaseMode == "scan" -> "Place the phone on a different side of the same object, then capture again."
                                            showcaseSide == 1 -> "Set the object on a stable surface, then capture Side 1."
                                            else -> "Side 2 was not captured. Position the object and capture again."
                                        },
                                        color = Muted,
                                        lineHeight = 21.sp
                                    )
                                    Button(onClick = { startShowcaseSide() }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                        Text(if (microphoneGranted) "Capture Side $showcaseSide" else "Enable microphone")
                                    }
                                }
                                "capturing" -> {
                                    Text("Capturing Side $showcaseSide", color = Cyan, fontSize = 18.sp)
                                    if (busy) {
                                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = Cyan)
                                        Text(capturePhase(stage), color = Cyan)
                                        if (!committing) OutlinedButton(onClick = { captureJob?.cancel() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel capture") }
                                    } else {
                                        TextButton(onClick = { showcaseStep = "capture" }) { Text("Return to capture controls") }
                                    }
                                }
                                "review" -> {
                                    val captured = pendingShowcaseScan
                                    val measured = pendingShowcaseEvidence
                                    if (captured != null && measured != null) {
                                        Text("Side $showcaseSide · measured response", color = Copper, fontSize = 20.sp)
                                        Waveform(captured.pcm)
                                        ShowcaseEvidenceCard("Side $showcaseSide", measured)
                                        Text("Approve to save, retry to record this side again, or reject to end this run.", color = Muted, lineHeight = 21.sp)
                                        if (busy) {
                                            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Cyan)
                                            Text(if (committing) "Saving approved capture" else capturePhase(stage), color = Cyan)
                                        }
                                        Button(onClick = {
                                            val sideLabel = "Side $showcaseSide"
                                            approveShowcaseCapture { approvedEvidence, path ->
                                                showcaseEvidence = showcaseEvidence + (sideLabel to approvedEvidence)
                                                showcasePaths = showcasePaths + path
                                                if (showcaseSide == 2) {
                                                    showcaseFingerprint = researchFingerprint(showcasePaths + path)
                                                }
                                                pendingShowcaseScan = null
                                                pendingShowcaseEvidence = null
                                                if (showcaseSide == 1) {
                                                    showcaseSide = 2
                                                    showcaseStep = "rotate"
                                                } else showcaseStep = "result"
                                            }
                                        }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                            Text(if (showcaseSide == 1) "Approve Side 1" else "Approve and finish")
                                        }
                                        OutlinedButton(onClick = { startShowcaseSide() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                                            Text("Retry Side $showcaseSide")
                                        }
                                        TextButton(onClick = {
                                            val rejectedSide = showcaseSide
                                            val approvedBefore = showcaseEvidence.isNotEmpty()
                                            pendingShowcaseScan = null
                                            pendingShowcaseEvidence = null
                                            showcaseMode = null
                                            showcaseStep = "name"
                                            if (rejectedSide == 2 && approvedBefore) {
                                                problem = "Side 2 was rejected; the approved Side 1 capture remains saved as research data."
                                            }
                                            log("Showcase Side $rejectedSide rejected; run ended")
                                        }, enabled = !busy) { Text("Reject and end") }
                                    }
                                }
                                "rotate" -> {
                                    Text("Rotate the object to expose the opposite side. Keep the phone and surface still.", color = Sand, lineHeight = 24.sp)
                                    Button(onClick = { startShowcaseSide() }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                        Text(if (microphoneGranted) "Capture Side 2" else "Enable microphone")
                                    }
                                }
                                "result" -> AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(420)) +
                                        slideInVertically(animationSpec = tween(420)) { it / 10 }
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        if (showcaseMode == "scan") {
                                            Text("INCONCLUSIVE", color = Copper, fontFamily = FontFamily.Serif, fontSize = 32.sp)
                                            Text("No known identity was established.", color = Sand, fontSize = 18.sp)
                                            Text(
                                                if (availableNames.isEmpty()) {
                                                    "This anonymous scan is saved as research data. No saved enrollments are available to identify it."
                                                } else {
                                                    "${availableNames.size} saved enrollment label(s) exist, but validated recognition models are unavailable; this scan was not compared or identified."
                                                },
                                                color = Muted,
                                                lineHeight = 21.sp
                                            )
                                        } else {
                                            Text("Added to research captures", color = Copper, fontFamily = FontFamily.Serif, fontSize = 28.sp)
                                            Text("Two real sides were recorded for $showcaseName.", color = Sand, fontSize = 18.sp)
                                        }
                                        Text("Capture provenance digest · not an identity signature", color = Cyan, fontSize = 16.sp)
                                        showcaseFingerprint?.let { Text("Measured digest: ${it.take(24)}…", color = Muted, fontSize = 12.sp) }
                                        showcaseEvidence.forEach { (side, measured) -> ShowcaseEvidenceCard(side, measured) }
                                        Text("Trained specialists, Laya, and calibration are unavailable. No identity decision or confidence is reported.", color = Muted, lineHeight = 21.sp)
                                        Button(onClick = {
                                            showcaseMode = null
                                            showcaseStep = "name"
                                            pendingShowcaseScan = null
                                            pendingShowcaseEvidence = null
                                        }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Done") }
                                    }
                                }
                            }
                        }
                        else -> {
                            Spacer(Modifier.height(30.dp))
                            Text("Give the physical\nworld a memory.", fontFamily = FontFamily.Serif, fontSize = 37.sp, lineHeight = 44.sp)
                            Text("Sound. Vibration. Physical evidence.", color = Muted, fontSize = 15.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { openShowcase("enroll") }, modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)) {
                                Text("Enroll object", fontSize = 17.sp)
                            }
                            OutlinedButton(onClick = { openShowcase("scan") }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                                Text("Scan object")
                            }
                            if (availableNames.isNotEmpty()) {
                                HorizontalDivider(color = Color(0xFF394247))
                                Text("Saved research names", color = Copper, fontSize = 20.sp)
                                availableNames.take(4).forEach { Text(it, color = Sand, fontSize = 16.sp) }
                            }
                            TextButton(onClick = { page = "capture" }, modifier = Modifier.fillMaxWidth()) {
                                Text("Four-position research capture")
                            }
                        }
                    }
                    problem?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    private fun startCapture(objectId: String, sessionId: String, condition: String, position: String) {
        if (busy) return
        val label = try { CaptureLabel(objectId.trim(), sessionId.trim(), condition, position) }
        catch (error: IllegalArgumentException) { problem = "Check the object and session IDs in the menu."; log(error.message ?: "Invalid label"); return }
        captureJob = lifecycleScope.launch {
            busy = true; problem = null
            try {
                val captured = sdk.capture(label) { actual -> lifecycleScope.launch { stage = actual; log(actual) } }
                stage = "Analyzing measured response"; log(stage)
                val measured = withContext(Dispatchers.Default) { sdk.analyzePhysics(captured) }
                val path = withContext(Dispatchers.IO) { sdk.saveCapture(captured) }
                val sameState = bundle.all { it.first.objectId == label.objectId && it.first.sessionId == label.sessionId && it.first.condition == label.condition }
                bundle = (if (sameState) bundle.filter { it.first.position != position } else emptyList()) + (label to path)
                scan = captured; evidence = measured
                savedCount = withContext(Dispatchers.IO) { sdk.listCaptures().size }
                stage = "Position $position saved"; log(stage)
            } catch (cancelled: CancellationException) {
                stage = "Capture cancelled"; log(stage); throw cancelled
            } catch (error: Exception) {
                stage = "Capture failed"; problem = "Capture could not finish. See logs in the menu."; log("$stage: ${error.message}")
            } finally { busy = false }
        }
    }
    private fun startShowcaseCapture(
        objectName: String,
        sessionId: String,
        mode: String,
        side: Int,
        onFinished: (Boolean) -> Unit,
    ) {
        if (busy) return
        val condition = if (mode == "enroll") "showcase_enrollment" else "showcase_scan"
        val label = try { CaptureLabel(objectName, sessionId, condition, "Side $side") }
        catch (error: IllegalArgumentException) {
            problem = "Invalid capture details; return and try again."
            log(error.message ?: "Invalid showcase capture label")
            onFinished(false)
            return
        }
        captureJob = lifecycleScope.launch {
            busy = true
            committing = false
            problem = null
            pendingShowcaseScan = null
            pendingShowcaseEvidence = null
            try {
                val captured = sdk.capture(label) { actual -> lifecycleScope.launch { stage = actual; log(actual) } }
                stage = "Analyzing measured response"
                log(stage)
                pendingShowcaseEvidence = withContext(Dispatchers.Default) { sdk.analyzePhysics(captured) }
                pendingShowcaseScan = captured
                stage = "Side $side ready for review"
                log(stage)
                onFinished(true)
            } catch (cancelled: CancellationException) {
                stage = "Capture cancelled"
                log(stage)
                onFinished(false)
                throw cancelled
            } catch (error: Exception) {
                stage = "Capture failed"
                problem = "Capture could not finish. See logs in the menu."
                log("$stage: ${error.message}")
                onFinished(false)
            } finally {
                busy = false
            }
        }
    }

    private fun approveShowcaseCapture(onSaved: (PhysicsEvidence, String) -> Unit) {
        if (busy) return
        val captured = pendingShowcaseScan ?: return
        val measured = pendingShowcaseEvidence ?: return
        lifecycleScope.launch {
            busy = true
            committing = true
            problem = null
            try {
                val path = withContext(Dispatchers.IO) { sdk.saveCapture(captured) }
                savedCount = withContext(Dispatchers.IO) { sdk.listCaptures().size }
                if (captured.label.condition == "showcase_enrollment") {
                    savedObjectNames = (savedObjectNames + captured.label.objectId).distinct()
                }
                stage = "${captured.label.position} saved"
                log(stage)
                onSaved(measured, path)
            } catch (cancelled: CancellationException) {
                stage = "Capture save cancelled"
                log(stage)
                throw cancelled
            } catch (error: Exception) {
                stage = "Capture save failed"
                problem = "Capture could not be saved. See logs in the menu."
                log("$stage: ${error.message}")
            } finally {
                busy = false
                committing = false
            }
        }
    }


    private fun saveBaseline(paths: List<String>) {
        if (busy) return
        lifecycleScope.launch {
            busy = true; committing = true; problem = null
            try {
                val reference = withContext(Dispatchers.IO) { SignatureVault(this@MainActivity).saveResearchBaseline(paths) }
                bundle = emptyList()
                baselines = listOf(reference) + baselines
                stage = "Measured baseline saved"; log(stage)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { problem = "Baseline could not be saved. See logs in the menu."; log("Baseline save failed: ${error.message}") }
            finally { busy = false; committing = false }
        }
    }
}

private fun capturePhase(stage: String): String = when {
    stage.contains("baseline") -> "Listening"
    stage.contains("chirp") -> "Acoustic scan"
    stage.contains("haptic") -> "Vibration scan"
    stage.contains("decay") -> "Recording response"
    stage.contains("Analyzing") -> "Processing response"
    else -> "Acquiring response"
}

@Composable private fun Metric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(value, color = Sand, fontSize = 19.sp)
        Text(label, color = Muted, fontSize = 11.sp)
    }
}
@Composable
private fun ShowcaseEvidenceCard(side: String, evidence: PhysicsEvidence) {
    Column(
        Modifier.fillMaxWidth().background(Charcoal).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(side, color = Copper, fontSize = 18.sp)
        Text("Audio RMS: ${format(evidence.audioRms)} · Noise RMS: ${format(evidence.noiseRms)}", color = Sand)
        Text("Peak: ${format(evidence.peak)} · Clipped samples: ${evidence.clippedSamples}", color = Muted, fontSize = 13.sp)
        Text("Acceleration RMS: ${format(evidence.accelerationRms)} · Gyroscope RMS: ${format(evidence.gyroRms)}", color = Sand)
        Text("Acoustic band energy: ${evidence.acousticBandEnergy.joinToString(" · ") { format(it) }}", color = Muted, fontSize = 12.sp)
        Text("Sample rates · accelerometer: ${format(evidence.accelerometerHz)} Hz · gyroscope: ${format(evidence.gyroscopeHz)} Hz", color = Muted, fontSize = 12.sp)
        Text("Duration: ${format(evidence.durationSeconds)} s", color = Muted, fontSize = 13.sp)
    }
}


@Composable private fun PositionGuide(position: String) {
    Column(Modifier.fillMaxWidth().background(Charcoal).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(position, color = Copper, fontFamily = FontFamily.Serif, fontSize = 52.sp)
        Text("Place the phone at mark $position.", color = Sand, fontSize = 18.sp)
        Text("Keep the case closed and the phone still.", color = Muted, fontSize = 13.sp)
    }
}

@Composable private fun Waveform(pcm: ShortArray) {
    Canvas(Modifier.fillMaxWidth().height(110.dp).background(Charcoal).semantics { contentDescription = "Waveform from the latest real microphone recording" }) {
        drawLine(Color(0xFF394247), Offset(0f, size.height / 2), Offset(size.width, size.height / 2))
        val columns = size.width.toInt().coerceAtLeast(1)
        for (column in 0 until columns) {
            val start = column * pcm.size / columns
            val end = ((column + 1) * pcm.size / columns).coerceAtLeast(start + 1).coerceAtMost(pcm.size)
            if (start >= end) continue
            var low = 0; var high = 0
            for (i in start until end) { low = minOf(low, pcm[i].toInt()); high = maxOf(high, pcm[i].toInt()) }
            drawLine(Cyan, Offset(column.toFloat(), size.height * (0.5f - high / 65536f)), Offset(column.toFloat(), size.height * (0.5f - low / 65536f)))
        }
    }
}

private fun format(value: Double): String = if (value.isFinite()) String.format(Locale.ROOT, "%.3f", value) else "unavailable"

private fun researchFingerprint(paths: List<String>): String {
    require(paths.size == 2)
    val source = paths.joinToString("|") { path ->
        val metadata = JSONObject(File(path, "metadata.json").readText())
        "${metadata.getString("captureId")}:${metadata.getString("audioSha256")}:${metadata.getString("imuSha256")}"
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(source.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}
