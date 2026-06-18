package com.yourpax.app.ui.screens.store

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.repository.EvilApRepository
import com.yourpax.app.data.repository.LootRepository
import com.yourpax.app.data.repository.SystemRepository
import com.yourpax.app.ui.components.*
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen() {
    val repo = remember { LootRepository() }
    val systemRepo = remember { SystemRepository() }
    val evilRepo = remember { EvilApRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var storeData by remember { mutableStateOf<StoreDataFull?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var expandedSection by remember { mutableStateOf<String?>(null) }

    var portalList by remember { mutableStateOf<List<PortalInfo>>(emptyList()) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var uploadStatus by remember { mutableStateOf("") }
    var deleteStatus by remember { mutableStateOf("") }
    var showDeletePortalConfirm by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        storeData = repo.getStoreData().getOrNull()
        portalList = evilRepo.listPortals().getOrNull()?.portals ?: emptyList()
        isLoading = false
    }

    LaunchedEffect(Unit) { refresh() }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            storeData = repo.getStoreData().getOrNull()
            portalList = evilRepo.listPortals().getOrNull()?.portals ?: emptyList()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        if (uri != null) {
            scope.launch {
                uploadStatus = "Uploading..."
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: return@launch
                    val fileName = uri.lastPathSegment ?: "portal.html"
                    systemRepo.uploadPortal(bytes, fileName).onSuccess {
                        uploadStatus = "Uploaded $fileName"
                        portalList = evilRepo.listPortals().getOrNull()?.portals ?: emptyList()
                    }.onFailure {
                        uploadStatus = "Upload failed: ${it.message}"
                    }
                } catch (e: Exception) {
                    uploadStatus = "Error: ${e.message}"
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Captured Store", fontWeight = FontWeight.SemiBold) },
            actions = {
                IconButton(onClick = { scope.launch { isLoading = true; refresh() } }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (isLoading) {
            LoadingOverlay()
        } else if (storeData == null) {
            EmptyState(title = "Could not load store data")
        } else {
            val data = storeData!!
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            val total = data.handshakes.size + data.pmkid.size + data.credsEvil.size + data.wpsReports.size + data.credsCracked.count + data.stolenFiles.size + data.scanResults.size + data.vulnScans.size + data.zombies.size + data.netkbCount

            Text("Captured Store ($total)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCardSm("${data.handshakes.size}", "Handshakes", Primary)
                StatCardSm("${data.pmkid.size}", "PMKID", Color(0xFFCC6600))
                StatCardSm("${data.wpsReports.size}", "WPS", Color(0xFFFF6600))
                StatCardSm("${data.credsEvil.size}", "Evil", Color(0xFFCC3333))
                StatCardSm("${data.credsCracked.count}", "Cracked", Success)
                StatCardSm("${data.stolenFiles.size}", "Files", Color(0xFF9933CC))
                StatCardSm("${data.scanResults.size}", "Scans", Color(0xFF3399FF))
                StatCardSm("${data.vulnScans.size}", "Vulns", Color(0xFFFF3366))
                StatCardSm("${data.zombies.size}", "Zombies", Color(0xFF33FF99))
                StatCardSm("${data.netkbCount}", "NetKB", Color(0xFF66CCFF))
            }

            StoreSection(title = "Handshake Captures (${data.handshakes.size})", accentColor = Primary,
                isEmpty = data.handshakes.isEmpty(), emptyText = "No handshake captures yet",
                expanded = expandedSection == "hs", onToggle = { expandedSection = if (it) "hs" else null }) {
                data.handshakes.forEach { item -> StoreFileRowWithDownload(item, repo) }
            }

            StoreSection(title = "PMKID Captures (${data.pmkid.size})", accentColor = Color(0xFFCC6600),
                isEmpty = data.pmkid.isEmpty(), emptyText = "No PMKID captures yet",
                expanded = expandedSection == "pmkid", onToggle = { expandedSection = if (it) "pmkid" else null }) {
                data.pmkid.forEach { item -> StoreFileRowWithDownload(item, repo) }
            }

            StoreSection(title = "Evil AP Captured Credentials (${data.credsEvil.size})", accentColor = Color(0xFFCC3333),
                isEmpty = data.credsEvil.isEmpty(), emptyText = "No credentials captured yet",
                expanded = expandedSection == "evil", onToggle = { expandedSection = if (it) "evil" else null }) {
                Column(Modifier.fillMaxWidth().background(Color(0xFF0D0D0D), RoundedCornerShape(4.dp)).padding(8.dp)) {
                    data.credsEvil.forEach { cred ->
                        Text("${cred.time} | ${cred.password}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color(0xFFFF6666))
                    }
                }
            }

            StoreSection(title = "WPS Attack Results (${data.wpsReports.size})", accentColor = Color(0xFFFF6600),
                isEmpty = data.wpsReports.isEmpty(), emptyText = "No WPS attacks run yet",
                expanded = expandedSection == "wps", onToggle = { expandedSection = if (it) "wps" else null }) {
                data.wpsReports.forEach { report ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)), shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(8.dp)) {
                            Text(report.file, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF6600), style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Text(report.content.take(500), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color(0xFF00FF00))
                        }
                    }
                }
            }

            StoreSection(title = "Cracked Service Credentials (${data.credsCracked.count})", accentColor = Success,
                isEmpty = data.credsCracked.count == 0, emptyText = "No cracked credentials yet",
                expanded = expandedSection == "cred", onToggle = { expandedSection = if (it) "cred" else null }) {
                data.credsCracked.services.forEach { svc ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(svc.name, style = MaterialTheme.typography.bodyMedium)
                        Text("${svc.count}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Success)
                        DownloadButton(onDownload = { repo.downloadStoreFile("data/output/crackedpwd/${svc.name.lowercase()}.csv") })
                    }
                    HorizontalDivider()
                }
            }

            StoreSection(title = "Stolen Files (${data.stolenFiles.size})", accentColor = Color(0xFF9933CC),
                isEmpty = data.stolenFiles.isEmpty(), emptyText = "No stolen files yet",
                expanded = expandedSection == "stolen", onToggle = { expandedSection = if (it) "stolen" else null }) {
                val tree = remember(data.stolenFiles) { buildFileTree(data.stolenFiles) }
                FileTreeView(tree = tree, expandedDirs = remember { mutableStateMapOf() }, level = 0, repo = repo)
            }

            StoreSection(title = "Network Scan Results (${data.scanResults.size})", accentColor = Color(0xFF3399FF),
                isEmpty = data.scanResults.isEmpty(), emptyText = "No network scan results yet",
                expanded = expandedSection == "scans", onToggle = { expandedSection = if (it) "scans" else null }) {
                data.scanResults.forEach { item -> StoreFileRowWithDownload(item, repo) }
            }

            StoreSection(title = "Vulnerability Scans (${data.vulnScans.size})", accentColor = Color(0xFFFF3366),
                isEmpty = data.vulnScans.isEmpty(), emptyText = "No vulnerability scans yet",
                expanded = expandedSection == "vulns", onToggle = { expandedSection = if (it) "vulns" else null }) {
                data.vulnScans.forEach { item ->
                    val ext = item.name.split(".").lastOrNull()?.uppercase() ?: ""
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.background(Color(0xFF444488), RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(ext, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = Color.White)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${item.size} \u00b7 ${item.modified}", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                        }
                        DownloadButton(onDownload = { repo.downloadStoreFile(item.path) })
                    }
                    HorizontalDivider()
                }
            }

            StoreSection(title = "Zombies (${data.zombies.size})", accentColor = Color(0xFF33FF99),
                isEmpty = data.zombies.isEmpty(), emptyText = "No zombie records yet",
                expanded = expandedSection == "zombies", onToggle = { expandedSection = if (it) "zombies" else null }) {
                data.zombies.forEach { item -> StoreFileRowWithDownload(item, repo) }
            }

            StoreSection(title = "Network Knowledge Base ($data.netkbCount)", accentColor = Color(0xFF66CCFF),
                isEmpty = data.netkbCount == 0, emptyText = "No network KB entries yet",
                expanded = expandedSection == "netkb", onToggle = { expandedSection = if (it) "netkb" else null }) {
                Text("${data.netkbCount} entries", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF66CCFF), modifier = Modifier.fillMaxWidth().padding(16.dp))
            }

            StoreSection(title = "Portal Templates (${portalList.size})", accentColor = Color(0xFF8833CC),
                isEmpty = portalList.isEmpty(), emptyText = "No portal templates",
                expanded = expandedSection == "portal", onToggle = { expandedSection = if (it) "portal" else null }) {
                if (uploadStatus.isNotEmpty()) {
                    Text(uploadStatus, style = MaterialTheme.typography.bodySmall, color = if (uploadStatus.startsWith("Uploaded") || uploadStatus.startsWith("Deleted")) Success else Error)
                }
                Button(
                    onClick = { filePickerLauncher.launch("text/html") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) { Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Upload Portal Template") }
                Spacer(Modifier.height(8.dp))
                portalList.forEach { portal ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.WebAsset, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF8833CC))
                        Column(Modifier.weight(1f)) {
                            Text(portal.name, style = MaterialTheme.typography.bodyMedium)
                            Text(portal.source, style = MaterialTheme.typography.bodySmall, color = SubtleText)
                        }
                        IconButton(onClick = { showDeletePortalConfirm = portal.name }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Error, modifier = Modifier.size(18.dp)) }
                    }
                    HorizontalDivider()
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
    }

    if (showDeletePortalConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeletePortalConfirm = null },
            title = { Text("Delete Portal Template?") },
            text = { Text("Delete portal template '${showDeletePortalConfirm}'? This cannot be undone.") },
            confirmButton = { TextButton(onClick = { val name = showDeletePortalConfirm!!; showDeletePortalConfirm = null; scope.launch { systemRepo.deletePortal(name).onSuccess { deleteStatus = "Deleted $name"; portalList = evilRepo.listPortals().getOrNull()?.portals ?: emptyList() }.onFailure { deleteStatus = "Delete failed: ${it.message}" } } }) { Text("Delete", color = Error) } },
            dismissButton = { TextButton(onClick = { showDeletePortalConfirm = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatCardSm(count: String, label: String, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(8.dp), modifier = Modifier.width(80.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(8.dp)) {
            Text(count, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = SubtleText)
        }
    }
}

@Composable
private fun StoreSection(title: String, accentColor: Color, isEmpty: Boolean, emptyText: String, expanded: Boolean, onToggle: (Boolean) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 0.dp)) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(accentColor))
                Column(Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onToggle(!expanded) }) {
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (expanded) {
                        if (isEmpty) {
                            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = SubtleText, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
                        } else {
                            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), content = content)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreFileRowWithDownload(item: StoreFileItem, repo: LootRepository) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = SubtleText)
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.size} \u00b7 ${item.modified}", style = MaterialTheme.typography.bodySmall, color = SubtleText)
        }
        DownloadButton(onDownload = { repo.downloadStoreFile(item.path) })
    }
    HorizontalDivider()
}

@Composable
private fun DownloadButton(onDownload: suspend () -> Result<ByteArray>? = { null }) {
    val scope = rememberCoroutineScope()
    var dlState by remember { mutableStateOf<String?>(null) }
    OutlinedButton(
        onClick = {
            scope.launch {
                dlState = "DL..."
                dlState = try {
                    val result = onDownload()
                    if (result?.isSuccess == true) "\u2713" else "Failed"
                } catch (e: Exception) { "Failed" }
            }
        },
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(dlState ?: "DL", style = MaterialTheme.typography.bodySmall)
    }
}

private data class FileNode(val name: String, val children: MutableMap<String, FileNode> = mutableMapOf(), val files: MutableList<StoreFileItem> = mutableListOf())

private fun buildFileTree(items: List<StoreFileItem>): Map<String, FileNode> {
    val roots = mutableMapOf<String, FileNode>()
    items.forEach { item ->
        val parts = item.rel.replace("\\", "/").split("/").filter { it.isNotEmpty() }
        if (parts.isEmpty()) return@forEach
        if (parts.size == 1) {
            roots.getOrPut(parts[0]) { FileNode(parts[0]) }.files.add(item)
        } else {
            val root = roots.getOrPut(parts[0]) { FileNode(parts[0]) }
            var current = root
            for (i in 1 until parts.size - 1) {
                current = current.children.getOrPut(parts[i]) { FileNode(parts[i]) }
            }
            current.files.add(item)
        }
    }
    return roots
}

@Composable
private fun FileTreeView(tree: Map<String, FileNode>, expandedDirs: MutableMap<String, Boolean>, level: Int, repo: LootRepository? = null) {
    tree.forEach { (name, node) ->
        val hasChildren = node.children.isNotEmpty() || node.files.isNotEmpty()
        val key = name + level
        val isExpanded = expandedDirs[key] == true
        Row(modifier = Modifier.fillMaxWidth().padding(start = (level * 16).dp).clickable { expandedDirs[key] = !isExpanded }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (hasChildren) {
                Icon(if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp), tint = SubtleText)
            } else {
                Spacer(Modifier.width(14.dp))
            }
            Text(name, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color(0xFFFFCC00), fontWeight = FontWeight.Bold)
            if (hasChildren) {
                Text("(${node.files.size + node.children.values.sumOf { it.files.size }} files)", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = SubtleText)
            }
        }
        if (isExpanded) {
            if (node.children.isNotEmpty()) {
                FileTreeView(tree = node.children, expandedDirs = expandedDirs, level = level + 1, repo = repo)
            }
            node.files.forEach { file ->
                Row(modifier = Modifier.fillMaxWidth().padding(start = ((level + 1) * 16 + 14).dp).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(file.name, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color(0xFFAAAAAA))
                    Text("(${file.size})", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = SubtleText)
                    Spacer(Modifier.weight(1f))
                    DownloadButton(onDownload = { repo?.downloadStoreFile(file.path) })
                }
            }
        }
    }
}
