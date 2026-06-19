package com.yourpax.app.ui.screens.loot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourpax.app.data.api.models.CredentialFile
import com.yourpax.app.data.api.models.LootFile
import com.yourpax.app.data.api.models.StoreDataFull
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.repository.LootRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.EmptyState
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.components.ModernCard
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LootScreen(
    onOpenDrawer: () -> Unit = {}
) {
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
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Credentials") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Files") })
        }

        if (isLoading) {
            LoadingOverlay()
        } else {
            when (selectedTab) {
                0 -> CredentialsTab(credentials = credentials, repo = repo, fontSize = fontSize)
                1 -> FilesTab(lootFiles = lootFiles, repo = repo, fontSize = fontSize)
            }
        }
    }
}

@Composable
private fun CredentialsTab(credentials: List<CredentialFile>, repo: LootRepository, fontSize: Float) {
    val scope = rememberCoroutineScope()
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
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp).clickable {
                                scope.launch {
                                    repo.downloadFile(file.name)
                                }
                            }
                        )
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
                        Text("+${file.rows.size - 10} more", style = MaterialTheme.typography.bodySmall, fontSize = fontSize.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun FilesTab(lootFiles: List<LootFile>, repo: LootRepository, fontSize: Float) {
    val scope = rememberCoroutineScope()
    if (lootFiles.isEmpty()) {
        EmptyState(title = "No stolen files", subtitle = "Run steal action to collect files")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(lootFiles) { file ->
            FileTreeItem(file = file, depth = 0, repo = repo, scope = scope, fontSize = fontSize)
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun FileTreeItem(file: LootFile, depth: Int, repo: LootRepository, scope: kotlinx.coroutines.CoroutineScope, fontSize: Float = 12f) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = (depth * 16).dp),
        shape = MaterialTheme.shapes.small,
        color = rememberAppColors().surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = if (file.isDirectory) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = fontSize.sp,
                modifier = Modifier.weight(1f)
            )
            if (!file.isDirectory) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp).clickable {
                        scope.launch {
                            repo.downloadFile(file.path)
                        }
                    }
                )
            }
        }
    }

    file.children.forEach { child ->
        FileTreeItem(file = child, depth = depth + 1, repo = repo, scope = scope, fontSize = fontSize)
    }
}
