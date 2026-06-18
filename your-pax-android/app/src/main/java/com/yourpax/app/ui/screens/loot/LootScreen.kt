package com.yourpax.app.ui.screens.loot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.api.models.CredentialFile
import com.yourpax.app.data.api.models.LootFile
import com.yourpax.app.data.api.models.StoreDataFull
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.repository.LootRepository
import com.yourpax.app.ui.components.*
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LootScreen() {
    val repo = remember { LootRepository() }
    var credentials by remember { mutableStateOf<List<CredentialFile>>(emptyList()) }
    var lootFiles by remember { mutableStateOf<List<LootFile>>(emptyList()) }
    var storeData by remember { mutableStateOf<StoreDataFull?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var fontSize by remember { mutableFloatStateOf(12f) }

    LaunchedEffect(Unit) {
        credentials = repo.getCredentials().getOrDefault(emptyList())
        lootFiles = repo.getLootFiles().getOrDefault(emptyList())
        storeData = repo.getStoreData().getOrNull()
        isLoading = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(20000)
            credentials = repo.getCredentials().getOrDefault(emptyList())
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            lootFiles = repo.getLootFiles().getOrDefault(emptyList())
            storeData = repo.getStoreData().getOrNull()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Loot", fontWeight = FontWeight.SemiBold) },
            actions = {
                IconButton(onClick = { fontSize = maxOf(8f, fontSize - 1f) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.TextDecrease, contentDescription = "Decrease", modifier = Modifier.size(16.dp), tint = SubtleText)
                }
                Text("${fontSize.toInt()}px", style = MaterialTheme.typography.labelSmall, color = SubtleText)
                IconButton(onClick = { fontSize = minOf(24f, fontSize + 1f) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.TextIncrease, contentDescription = "Increase", modifier = Modifier.size(16.dp), tint = SubtleText)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Primary
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Credentials") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Files") })
        }

        if (isLoading) {
            LoadingOverlay()
        } else {
            when (selectedTab) {
                0 -> CredentialsTab(credentials = credentials, fontSize = fontSize)
                1 -> FilesTab(lootFiles = lootFiles, fontSize = fontSize)
            }
        }
    }
}

@Composable
private fun CredentialsTab(credentials: List<CredentialFile>, fontSize: Float) {
    if (credentials.isEmpty()) {
        EmptyState(title = "No credentials found", subtitle = "Run bruteforce to capture credentials")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(credentials) { file ->
            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = fontSize.sp)
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = Primary, modifier = Modifier.size(20.dp))
                    }
                    HorizontalDivider()
                    file.rows.take(10).forEach { row ->
                        Text(
                            text = row.joinToString(" | "),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = fontSize.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (file.rows.size > 10) {
                        Text("+${file.rows.size - 10} more", style = MaterialTheme.typography.bodySmall, fontSize = fontSize.sp, color = SubtleText)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun FilesTab(lootFiles: List<LootFile>, fontSize: Float) {
    if (lootFiles.isEmpty()) {
        EmptyState(title = "No stolen files", subtitle = "Run steal action to collect files")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(lootFiles) { file ->
            FileTreeItem(file = file, depth = 0, fontSize = fontSize)
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun FileTreeItem(file: LootFile, depth: Int, fontSize: Float = 12f) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = if (file.isDirectory) Warning else Info,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = fontSize.sp,
                modifier = Modifier.weight(1f)
            )
            if (!file.isDirectory) {
                Icon(Icons.Default.Download, contentDescription = "Download", tint = Primary, modifier = Modifier.size(18.dp))
            }
        }
    }

    file.children.forEach { child ->
        FileTreeItem(file = child, depth = depth + 1, fontSize = fontSize)
    }
}
