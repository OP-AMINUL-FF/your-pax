package com.yourpax.app.ui.screens.store

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourpax.app.data.api.models.PortalInfo
import com.yourpax.app.data.api.models.StoreDataFull
import com.yourpax.app.data.api.models.StoreFileItem
import com.yourpax.app.data.repository.EvilApRepository
import com.yourpax.app.data.repository.LootRepository
import com.yourpax.app.data.repository.SystemRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.EmptyState
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.theme.AppColors
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    onOpenDrawer: () -> Unit = {}
) {
    val repo = remember { LootRepository() }
    val systemRepo = remember { SystemRepository() }
    val evilRepo = remember { EvilApRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val appColors = rememberAppColors()

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
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
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
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val total = data.handshakes.size + data.pmkid.size + data.credsEvil.size + data.wpsReports.size + data.credsCracked.count + data.stolenFiles.size + data.scanResults.size + data.vulnScans.size + data.zombies.size + data.netkbCount

                Text("Captured Store ($total)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCardSm(count = "${data.handshakes.size}", label = "Handshakes", color = appColors.categoryHandshake)
                    StatCardSm(count = "${data.pmkid.size}", label = "PMKID", color = appColors.categoryPmkid)
                    StatCardSm(count = "${data.wpsReports.size}", label = "WPS", color = appColors.categoryWps)
                    StatCardSm(count = "${data.credsEvil.size}", label = "Evil", color = appColors.categoryEvilCreds)
                    StatCardSm(count = "${data.credsCracked.count}", label = "Cracked", color = appColors.categoryCracked)
                    StatCardSm(count = "${data.stolenFiles.size}", label = "Files", color = appColors.categoryStolen)
                    StatCardSm(count = "${data.scanResults.size}", label = "Scans", color = appColors.categoryScan)
                    StatCardSm(count = "${data.vulnScans.size}", label = "Vulns", color = appColors.categoryVuln)
                    StatCardSm(count = "${data.zombies.size}", label = "Zombies", color = appColors.categoryZombie)
                    StatCardSm(count = "${data.netkbCount}", label = "NetKB", color = appColors.categoryNetkb)
                }

                StoreSection(title = "Handshake Captures (${data.handshakes.size})", accentColor = appColors.categoryHandshake,
                    isEmpty = data.handshakes.isEmpty(), emptyText = "No handshake captures yet",
                    expanded = expandedSection == "hs", onToggle = { expandedSection = if (it) "hs" else null }) {
                    data.handshakes.forEach { item -> StoreFileRowWithDownload(item, repo, appColors) }
                }

                StoreSection(title = "PMKID Captures (${data.pmkid.size})", accentColor = appColors.categoryPmkid,
                    isEmpty = data.pmkid.isEmpty(), emptyText = "No PMKID captures yet",
                    expanded = expandedSection == "pmkid", onToggle = { expandedSection = if (it) "pmkid" else null }) {
                    data.pmkid.forEach { item -> StoreFileRowWithDownload(item, repo, appColors) }
                }

                StoreSection(title = "Evil AP Captured Credentials (${data.credsEvil.size})", accentColor = appColors.categoryEvilCreds,
                    isEmpty = data.credsEvil.isEmpty(), emptyText = "No credentials captured yet",
                    expanded = expandedSection == "evil", onToggle = { expandedSection = if (it) "evil" else null }) {
                    Column(Modifier.fillMaxWidth().background(appColors.terminalBackground, MaterialTheme.shapes.extraSmall).padding(8.dp)) {
                        data.credsEvil.forEach { cred ->
                            Text("${cred.time} | ${cred.password}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = appColors.categoryEvilCreds)
                        }
                    }
                }

                StoreSection(title = "WPS Attack Results (${data.wpsReports.size})", accentColor = appColors.categoryWps,
                    isEmpty = data.wpsReports.isEmpty(), emptyText = "No WPS attacks run yet",
                    expanded = expandedSection == "wps", onToggle = { expandedSection = if (it) "wps" else null }) {
                    data.wpsReports.forEach { report ->
                        Surface(shape = MaterialTheme.shapes.extraSmall, color = appColors.terminalBackground, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(8.dp)) {
                                Text(report.file, fontWeight = FontWeight.SemiBold, color = appColors.categoryWps, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(4.dp))
                                Text(report.content.take(500), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = appColors.terminalText)
                            }
                        }
                    }
                }

                StoreSection(title = "Cracked Service Credentials (${data.credsCracked.count})", accentColor = appColors.categoryCracked,
                    isEmpty = data.credsCracked.count == 0, emptyText = "No cracked credentials yet",
                    expanded = expandedSection == "cred", onToggle = { expandedSection = if (it) "cred" else null }) {
                    data.credsCracked.services.forEach { svc ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(svc.name, style = MaterialTheme.typography.bodyMedium)
                            Text("${svc.count}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = appColors.categoryCracked)
                            DownloadButton(onDownload = { repo.downloadStoreFile("data/output/crackedpwd/${svc.name.lowercase()}.csv") })
                        }
                        HorizontalDivider()
                    }
                }

                StoreSection(title = "Stolen Files (${data.stolenFiles.size})", accentColor = appColors.categoryStolen,
                    isEmpty = data.stolenFiles.isEmpty(), emptyText = "No stolen files yet",
                    expanded = expandedSection == "stolen", onToggle = { expandedSection = if (it) "stolen" else null }) {
                    val tree = remember(data.stolenFiles) { buildFileTree(data.stolenFiles) }
                    FileTreeView(tree = tree, expandedDirs = remember { mutableMapOf() }, level = 0, repo = repo, appColors = appColors)
                }

                StoreSection(title = "Network Scan Results (${data.scanResults.size})", accentColor = appColors.categoryScan,
                    isEmpty = data.scanResults.isEmpty(), emptyText = "No network scan results yet",
                    expanded = expandedSection == "scans", onToggle = { expandedSection = if (it) "scans" else null }) {
                    data.scanResults.forEach { item -> StoreFileRowWithDownload(item, repo, appColors) }
                }

                StoreSection(title = "Vulnerability Scans (${data.vulnScans.size})", accentColor = appColors.categoryVuln,
                    isEmpty = data.vulnScans.isEmpty(), emptyText = "No vulnerability scans yet",
                    expanded = expandedSection == "vulns", onToggle = { expandedSection = if (it) "vulns" else null }) {
                    data.vulnScans.forEach { item ->
                        val ext = item.name.split(".").lastOrNull()?.uppercase() ?: ""
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.extraSmall).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(ext, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${item.size} \u00b7 ${item.modified}", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                            }
                            DownloadButton(onDownload = { repo.downloadStoreFile(item.path) })
                        }
                        HorizontalDivider()
                    }
                }

                StoreSection(title = "Zombies (${data.zombies.size})", accentColor = appColors.categoryZombie,
                    isEmpty = data.zombies.isEmpty(), emptyText = "No zombie records yet",
                    expanded = expandedSection == "zombies", onToggle = { expandedSection = if (it) "zombies" else null }) {
                    data.zombies.forEach { item -> StoreFileRowWithDownload(item, repo, appColors) }
                }

                StoreSection(title = "Network Knowledge Base ($data.netkbCount)", accentColor = appColors.categoryNetkb,
                    isEmpty = data.netkbCount == 0, emptyText = "No network KB entries yet",
                    expanded = expandedSection == "netkb", onToggle = { expandedSection = if (it) "netkb" else null }) {
                    Text("${data.netkbCount} entries", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = appColors.categoryNetkb, modifier = Modifier.fillMaxWidth().padding(16.dp))
                }

                StoreSection(title = "Portal Templates (${portalList.size})", accentColor = appColors.categoryPortal,
                    isEmpty = portalList.isEmpty(), emptyText = "No portal templates",
                    expanded = expandedSection == "portal", onToggle = { expandedSection = if (it) "portal" else null }) {
                    if (uploadStatus.isNotEmpty()) {
                        Text(uploadStatus, style = MaterialTheme.typography.bodySmall, color = if (uploadStatus.startsWith("Uploaded") || uploadStatus.startsWith("Deleted")) appColors.success else MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = { filePickerLauncher.launch("text/html") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    ) { Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Upload Portal Template") }
                    Spacer(Modifier.height(8.dp))
                    portalList.forEach { portal ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.WebAsset, contentDescription = null, modifier = Modifier.size(16.dp), tint = appColors.categoryPortal)
                            Column(Modifier.weight(1f)) {
                                Text(portal.name, style = MaterialTheme.typography.bodyMedium)
                                Text(portal.source, style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                            }
                            IconButton(onClick = { showDeletePortalConfirm = portal.name }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
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
            confirmButton = { TextButton(onClick = { val name = showDeletePortalConfirm!!; showDeletePortalConfirm = null; scope.launch { systemRepo.deletePortal(name).onSuccess { deleteStatus = "Deleted $name"; portalList = evilRepo.listPortals().getOrNull()?.portals ?: emptyList() }.onFailure { deleteStatus = "Delete failed: ${it.message}" } } }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeletePortalConfirm = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatCardSm(count: String, label: String, color: Color) {
    val appColors = rememberAppColors()
    Surface(shape = MaterialTheme.shapes.small, color = appColors.surfaceContainerLow, tonalElevation = 0.dp, modifier = Modifier.width(80.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(8.dp)) {
            Text(count, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = appColors.subtleText)
        }
    }
}

@Composable
private fun StoreSection(title: String, accentColor: Color, isEmpty: Boolean, emptyText: String, expanded: Boolean, onToggle: (Boolean) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val appColors = rememberAppColors()
    Surface(shape = MaterialTheme.shapes.small, color = appColors.surfaceContainerLow, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
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
                            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = appColors.subtleText, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
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
private fun StoreFileRowWithDownload(item: StoreFileItem, repo: LootRepository, appColors: AppColors) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = appColors.subtleText)
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.size} \u00b7 ${item.modified}", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
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
        shape = MaterialTheme.shapes.extraSmall
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
private fun FileTreeView(tree: Map<String, FileNode>, expandedDirs: MutableMap<String, Boolean>, level: Int, repo: LootRepository? = null, appColors: AppColors = rememberAppColors()) {
    tree.forEach { (name, node) ->
        val hasChildren = node.children.isNotEmpty() || node.files.isNotEmpty()
        val key = name + level
        val isExpanded = expandedDirs[key] == true
        Row(modifier = Modifier.fillMaxWidth().padding(start = (level * 16).dp).clickable { expandedDirs[key] = !isExpanded }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (hasChildren) {
                Icon(if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp), tint = appColors.subtleText)
            } else {
                Spacer(Modifier.width(14.dp))
            }
            Text(name, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = appColors.categoryStolen, fontWeight = FontWeight.Bold)
            if (hasChildren) {
                Text("(${node.files.size + node.children.values.sumOf { it.files.size }} files)", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = appColors.subtleText)
            }
        }
        if (isExpanded) {
            if (node.children.isNotEmpty()) {
                FileTreeView(tree = node.children, expandedDirs = expandedDirs, level = level + 1, repo = repo, appColors = appColors)
            }
            node.files.forEach { file ->
                Row(modifier = Modifier.fillMaxWidth().padding(start = ((level + 1) * 16 + 14).dp).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(file.name, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = appColors.subtleText)
                    Text("(${file.size})", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = appColors.subtleText)
                    Spacer(Modifier.weight(1f))
                    DownloadButton(onDownload = { repo?.downloadStoreFile(file.path) })
                }
            }
        }
    }
}
