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
        var introVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { introVisible = true }
        var showcaseMode by remember { mutableStateOf<String?>(null) }
        var showcaseName by remember { mutableStateOf("") }
        var showcaseCaptureId by remember { mutableStateOf("") }
        var showcaseSession by remember { mutableStateOf("") }
        var showcaseStep by remember { mutableStateOf("name") }
        var showcasePositionIndex by remember { mutableIntStateOf(0) }
        var showcaseReferenceSaved by remember { mutableStateOf(false) }
        var showcaseEvidence by remember { mutableStateOf<List<Pair<String, PhysicsEvidence>>>(emptyList()) }
        val researchProfile = remember { sdk.getProfile(ProfileEngine.RIGID_CASE_ID) }
        val positions = researchProfile.positions
        val showcasePositions = remember { sdk.getProfile(ProfileEngine.SHOWCASE_OBJECT_ID).positions }
        val showcasePosition = showcasePositions[showcasePositionIndex]
        val availableNames = baselines.map { it.objectId }.distinct()
        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        fun openShowcase(mode: String) {
            showcaseMode = mode
            showcaseName = ""
            showcaseCaptureId = if (mode == "scan") "candidate-${java.util.UUID.randomUUID()}" else ""
            showcaseSession = ""
            showcasePositionIndex = 0
            showcaseReferenceSaved = false
            showcaseStep = if (mode == "scan") "capture" else "name"
            showcaseEvidence = emptyList()
            showcasePaths = emptyList()
            showcaseFingerprint = null
            pendingShowcaseScan = null
            pendingShowcaseEvidence = null
            problem = null
        }

        fun startShowcasePosition() {
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
            startShowcaseCapture(captureObjectId, showcaseSession, mode, showcasePosition) { succeeded ->
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
                            Text("EchoNet, VibraNet, PhysFusion, StateNet and SpatialNet: checkpoints unavailable.\nCapybara Laya: not trained or installed.\nCalibration: pending.\nVerification: unavailable.", color = Muted, lineHeight = 21.sp)
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
                                    Text(
                                        "Name the object, then capture two real sides. Keep the phone flat and use the same contact pressure on both sides.",
                                        color = Muted, lineHeight = 21.sp
                                    )
                                    Text(if (showcaseMode == "enroll") "Create a research reference" else "Scan a known object", color = Sand, fontFamily = FontFamily.Serif, fontSize = 24.sp)
                                    OutlinedTextField(
                                        value = showcaseName,
                                        onValueChange = { showcaseName = it },
                                        label = { Text("Object name") },
                                        singleLine = true,
                                        enabled = !busy,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(onClick = { startShowcasePosition() }, enabled = showcaseName.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                        Text(if (microphoneGranted) "Capture side 1" else "Enable microphone")
                                    }
                                }
                                "capture" -> {
                                    Text("Place the phone on the object.", color = Sand, fontFamily = FontFamily.Serif, fontSize = 24.sp)
                                    Text("Side ${showcasePositionIndex + 1} of 2 · approve the measured response before continuing.", color = Cyan)
                                    ObjectGuide(showcasePositionIndex)
                                    Button(onClick = { startShowcasePosition() }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                        Text(if (microphoneGranted) "Capture side ${showcasePositionIndex + 1}" else "Enable microphone")
                                    }
                                }
                                "capturing" -> {
                                    Text("Capturing side ${showcasePositionIndex + 1}", color = Cyan, fontSize = 18.sp)
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
                                        Text("Side ${showcasePositionIndex + 1} · measured response", color = Copper, fontSize = 20.sp)
                                        Waveform(captured.pcm)
                                        ShowcaseEvidenceCard("Side ${showcasePositionIndex + 1}", measured)
                                        Text("Approve this real measurement, retry it, or reject the run.", color = Muted, lineHeight = 21.sp)
                                        if (busy) {
                                            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Cyan)
                                            Text(if (committing) "Saving approved capture" else capturePhase(stage), color = Cyan)
                                        }
                                        Button(onClick = {
                                            approveShowcaseCapture { approvedEvidence, path, baselineSaved ->
                                                showcaseEvidence = showcaseEvidence + ("Side ${showcasePositionIndex + 1}" to approvedEvidence)
                                                showcasePaths = showcasePaths + path
                                                showcaseReferenceSaved = baselineSaved
                                                pendingShowcaseScan = null
                                                pendingShowcaseEvidence = null
                                                if (showcasePositionIndex < showcasePositions.lastIndex) {
                                                    showcasePositionIndex++
                                                    showcaseStep = "rotate"
                                                } else {
                                                    showcaseFingerprint = researchFingerprint(showcasePaths)
                                                    showcaseStep = "result"
                                                }
                                            }
                                        }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                            Text(if (showcasePositionIndex == showcasePositions.lastIndex) "Approve final side" else "Approve side ${showcasePositionIndex + 1}")
                                        }
                                        OutlinedButton(onClick = { startShowcasePosition() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                                            Text("Retry side ${showcasePositionIndex + 1}")
                                        }
                                        TextButton(onClick = {
                                            pendingShowcaseScan = null
                                            pendingShowcaseEvidence = null
                                            showcaseMode = null
                                            showcaseStep = "name"
                                            problem = "This run was rejected; approved captures remain saved as research data."
                                            log("Showcase side ${showcasePositionIndex + 1} rejected; run ended")
                                        }, enabled = !busy) { Text("Reject and end") }
                                    }
                                }
                                "rotate" -> {
                                    ObjectGuide(showcasePositionIndex)
                                    Text("Scan the other side. Keep the object still while you turn the phone to the next contact surface.", color = Sand, lineHeight = 24.sp)
                                    Button(onClick = { startShowcasePosition() }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                        Text(if (microphoneGranted) "Scan other side" else "Enable microphone")
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
                                            Text("The physical response was captured, but no validated identity decision is available.", color = Sand, fontSize = 18.sp)
                                            Text(
                                                if (availableNames.isEmpty()) {
                                                    "Two measured sides were saved as private research data. Enroll an object first to create a comparison reference."
                                                } else {
                                                    "${availableNames.size} saved object(s) exist, but trained specialists and calibration are unavailable; this scan was not identified."
                                                },
                                                color = Muted, lineHeight = 21.sp
                                            )
                                        } else if (showcaseReferenceSaved) {
                                            Text("Object added", color = Copper, fontFamily = FontFamily.Serif, fontSize = 32.sp)
                                            Text("$showcaseName has a two-side research reference.", color = Sand, fontSize = 18.sp)
                                            Text("Both real measurements were approved and saved in an encrypted, uncalibrated reference. The digest records capture provenance, not physical identity.", color = Muted, lineHeight = 21.sp)
                                        } else {
                                            Text("Reference not saved", color = Copper, fontFamily = FontFamily.Serif, fontSize = 32.sp)
                                            Text("Both captures remain private research data, but no complete encrypted reference was created. This object is not listed as enrolled.", color = Sand, fontSize = 18.sp)
                                        }
                                        showcaseFingerprint?.let { Text("Measured digest: ${it.take(24)}…", color = Muted, fontSize = 12.sp) }
                                        showcaseEvidence.forEach { (position, measured) -> ShowcaseEvidenceCard(position, measured) }
                                        Text("Trained specialists, a Capybara Laya model, and calibration are unavailable. No identity decision or confidence is reported.", color = Muted, lineHeight = 21.sp)
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
                            AnimatedVisibility(
                                visible = introVisible,
                                enter = fadeIn(animationSpec = tween(700)) +
                                    slideInVertically(animationSpec = tween(700)) { it / 8 }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Give the physical\nworld a memory.", fontFamily = FontFamily.Serif, fontSize = 37.sp, lineHeight = 44.sp)
                                    Text("Sound. Vibration. Physical evidence.", color = Muted, fontSize = 15.sp)
                                }
                            }
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
        val label = try { CaptureLabel(objectId.trim(), sessionId.trim(), condition, position, ProfileEngine.RIGID_CASE_ID) }
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
        position: String,
        onFinished: (Boolean) -> Unit,
    ) {
        if (busy) return
        val condition = if (mode == "enroll") "showcase_enrollment" else "showcase_scan"
        val label = try { CaptureLabel(objectName, sessionId, condition, position, ProfileEngine.SHOWCASE_OBJECT_ID) }
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
                stage = "Position $position ready for review"
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

    private fun approveShowcaseCapture(onSaved: (PhysicsEvidence, String, Boolean) -> Unit) {
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
                val baselineSaved = if (
                    captured.label.condition == "showcase_enrollment" &&
                    captured.label.profile == ProfileEngine.SHOWCASE_OBJECT_ID &&
                    captured.label.position == ProfileEngine.get(captured.label.profile).positions.last()
                ) {
                    try {
                        val reference = createResearchBaseline(showcasePaths + path)
                        baselines = listOf(reference) + baselines
                        true
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        problem = "All registered captures were saved, but the encrypted research baseline could not be created."
                        log("Research baseline save failed: ${error.message}")
                        false
                    }
                } else {
                    false
                }
                stage = "${captured.label.position} saved"
                log(stage)
                onSaved(measured, path, baselineSaved)
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
                val reference = createResearchBaseline(paths)
                bundle = emptyList()
                baselines = listOf(reference) + baselines
                stage = "Measured baseline saved"; log(stage)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { problem = "Baseline could not be saved. See logs in the menu."; log("Baseline save failed: ${error.message}") }
            finally { busy = false; committing = false }
        }
    }

    private suspend fun createResearchBaseline(paths: List<String>): ResearchBaseline = withContext(Dispatchers.IO) {
        SignatureVault(this@MainActivity).saveResearchBaseline(paths)
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
private fun ShowcaseEvidenceCard(position: String, evidence: PhysicsEvidence) {
    Column(
        Modifier.fillMaxWidth().background(Charcoal).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(position, color = Copper, fontSize = 18.sp)
        Text("Audio RMS: ${format(evidence.audioRms)} · Noise RMS: ${format(evidence.noiseRms)}", color = Sand)
        Text("Peak: ${format(evidence.peak)} · Clipped samples: ${evidence.clippedSamples}", color = Muted, fontSize = 13.sp)
        Text("Acceleration RMS: ${format(evidence.accelerationRms)} · Gyroscope RMS: ${format(evidence.gyroRms)}", color = Sand)
        Text("Acoustic band energy: ${evidence.acousticBandEnergy.joinToString(" · ") { format(it) }}", color = Muted, fontSize = 12.sp)
        Text("Sample rates · accelerometer: ${format(evidence.accelerometerHz)} Hz · gyroscope: ${format(evidence.gyroscopeHz)} Hz", color = Muted, fontSize = 12.sp)
        Text("Duration: ${format(evidence.durationSeconds)} s", color = Muted, fontSize = 13.sp)
    }
}


@Composable
private fun ObjectGuide(sideIndex: Int) {
    Column(
        Modifier.fillMaxWidth().background(Charcoal).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("TWO-SIDE OBJECT SCAN", color = Cyan, fontSize = 11.sp, letterSpacing = 1.sp)
        Box(
            Modifier.fillMaxWidth().height(86.dp).background(Obsidian),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(if (sideIndex == 0) "PLACE PHONE ON SIDE 1" else "TURN TO SIDE 2", color = Sand, fontSize = 15.sp)
        }
        Text(
            if (sideIndex == 0) "Use any stable box or object. Keep the phone fully supported during the real acoustic and haptic capture."
            else "Scan the other contact surface without changing the object or measurement conditions.",
            color = Sand, fontSize = 16.sp, lineHeight = 22.sp
        )
    }
}

@Composable private fun PositionGuide(position: String) {
    Column(Modifier.fillMaxWidth().background(Charcoal).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("REGISTERED CONTACT POINTS · TOP VIEW", color = Cyan, fontSize = 11.sp, letterSpacing = 1.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PositionMarker("A", position == "A")
            PositionMarker("B", position == "B")
        }
        Box(Modifier.fillMaxWidth().height(52.dp).background(Obsidian), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("CLOSED CASE", color = Muted, fontSize = 13.sp, letterSpacing = 1.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PositionMarker("D", position == "D")
            PositionMarker("C", position == "C")
        }
        Text("Place the phone at mark $position. Keep the case closed and its orientation fixed.", color = Sand, fontSize = 16.sp, lineHeight = 22.sp)
    }
}

@Composable private fun PositionMarker(position: String, selected: Boolean) {
    Text(
        position,
        Modifier.background(if (selected) Copper else Obsidian).padding(horizontal = 18.dp, vertical = 8.dp),
        color = if (selected) Obsidian else Muted,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
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
