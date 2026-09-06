package com.satoya.novalauncher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { LauncherScreen() } }
    }
}

@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val repo = remember { LauncherRepository(context) }
    var apps by remember { mutableStateOf(repo.loadApps()) }
    var query by remember { mutableStateOf("") }
    var favorites by remember { mutableStateOf(repo.favorites()) }
    var folders by remember { mutableStateOf(repo.folders()) }
    var folderBeingEdited by remember { mutableStateOf<AppFolder?>(null) }
    var isCreatingFolder by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<AppFolder?>(null) }
    var expandedFolderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val shown = apps.filter { query.isBlank() || it.label.contains(query, true) }
    val folderAppKeys = folders.flatMapTo(mutableSetOf<String>()) { it.appKeys }
    val registered = shown.filter { it.key in favorites && it.key !in folderAppKeys }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Nova Launcher", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(query, { query = it }, label = { Text("アプリを検索") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        apps = repo.loadApps()
                        favorites = repo.favorites()
                        folders = repo.folders()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("更新") }
                Button(
                    onClick = { isCreatingFolder = true },
                    modifier = Modifier.weight(1f)
                ) { Text("フォルダ") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { requestHome(context) },
                    modifier = Modifier.weight(1f)
                ) { Text("既定のホームに設定", maxLines = 1) }
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) },
                    modifier = Modifier.weight(1f)
                ) { Text("設定") }
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("フォルダ", style = MaterialTheme.typography.titleLarge)
            }
            if (folders.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "フォルダボタンから、アプリをまとめるフォルダを作成できます",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(folders, key = { "folder-${it.id}" }) { folder ->
                    val folderApps = folder.appKeys.mapNotNull { key ->
                        apps.firstOrNull { it.key == key }
                    }.filter { query.isBlank() || it.label.contains(query, true) }
                    FolderBar(
                        folder = folder,
                        apps = folderApps,
                        expanded = folder.id in expandedFolderIds,
                        onToggleExpanded = {
                            expandedFolderIds = if (folder.id in expandedFolderIds) {
                                expandedFolderIds - folder.id
                            } else {
                                expandedFolderIds + folder.id
                            }
                        },
                        onEdit = { folderBeingEdited = folder },
                        onDelete = { folderToDelete = folder },
                        onLaunch = { app -> repo.launch(app) }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("登録済みアプリ", style = MaterialTheme.typography.titleLarge)
            }
            if (registered.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        if (query.isBlank()) "下のアプリ一覧から ☆ をタップして登録してください" else "検索条件に一致する登録済みアプリはありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(registered, key = { "registered-${it.key}" }) { app ->
                    AppBar(
                        app = app,
                        isRegistered = true,
                        onLaunch = { repo.launch(app) },
                        onToggleRegistration = {
                            repo.setFavorite(app, false)
                            favorites = repo.favorites()
                        }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "アプリ一覧",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(shown, key = { "all-${it.key}" }) { app ->
                AppBar(
                    app = app,
                    isRegistered = app.key in favorites,
                    onLaunch = { repo.launch(app) },
                    onToggleRegistration = {
                        repo.setFavorite(app, app.key !in favorites)
                        favorites = repo.favorites()
                    }
                )
            }
        }
    }

    if (isCreatingFolder || folderBeingEdited != null) {
        FolderDialog(
            folder = folderBeingEdited,
            apps = apps,
            onDismiss = {
                isCreatingFolder = false
                folderBeingEdited = null
            },
            onSave = { name, appKeys ->
                val folder = folderBeingEdited?.copy(name = name, appKeys = appKeys)
                    ?: AppFolder(UUID.randomUUID().toString(), name, appKeys)
                repo.saveFolder(folder)
                appKeys.forEach { key ->
                    apps.firstOrNull { it.key == key }?.let { repo.setFavorite(it, true) }
                }
                folders = repo.folders()
                favorites = repo.favorites()
                isCreatingFolder = false
                folderBeingEdited = null
            }
        )
    }

    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("フォルダを削除") },
            text = { Text("「${folder.name}」を削除しますか？中のアプリは削除されません。") },
            confirmButton = {
                TextButton(onClick = {
                    repo.deleteFolder(folder.id)
                    folders = repo.folders()
                    expandedFolderIds = expandedFolderIds - folder.id
                    folderToDelete = null
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) { Text("キャンセル") }
            }
        )
    }
}

@Composable
private fun FolderBar(
    folder: AppFolder,
    apps: List<AppEntry>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLaunch: (AppEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Card(
            onClick = onToggleExpanded,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(
                    folder.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${apps.size}個",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "フォルダを編集")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "フォルダを削除")
                }
            }
        }

        if (expanded) {
            if (apps.isEmpty()) {
                Text(
                    "このフォルダにアプリはありません",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                apps.forEach { app ->
                    AppBar(
                        app = app,
                        isRegistered = true,
                        onLaunch = { onLaunch(app) },
                        onToggleRegistration = null
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderDialog(
    folder: AppFolder?,
    apps: List<AppEntry>,
    onDismiss: () -> Unit,
    onSave: (String, Set<String>) -> Unit
) {
    var name by remember(folder?.id) { mutableStateOf(folder?.name.orEmpty()) }
    var appQuery by remember(folder?.id) { mutableStateOf("") }
    var selectedKeys by remember(folder?.id) { mutableStateOf(folder?.appKeys ?: emptySet()) }
    val filteredApps = apps.filter {
        appQuery.isBlank() || it.label.contains(appQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folder == null) "フォルダを作成" else "フォルダを編集") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("フォルダ名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("入れるアプリ", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    Text("${selectedKeys.size}個選択中", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(4.dp))
                if (apps.isEmpty()) {
                    Text("インストール済みアプリがありません")
                } else {
                    OutlinedTextField(
                        value = appQuery,
                        onValueChange = { appQuery = it },
                        label = { Text("アプリを検索") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        lazyItems(filteredApps, key = { it.key }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedKeys = if (app.key in selectedKeys) {
                                            selectedKeys - app.key
                                        } else {
                                            selectedKeys + app.key
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = app.key in selectedKeys,
                                    onCheckedChange = { checked ->
                                        selectedKeys = if (checked) {
                                            selectedKeys + app.key
                                        } else {
                                            selectedKeys - app.key
                                        }
                                    }
                                )
                                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (filteredApps.isEmpty()) {
                        Text(
                            "検索条件に一致するアプリはありません",
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), selectedKeys) },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

@Composable
private fun AppBar(
    app: AppEntry,
    isRegistered: Boolean,
    onLaunch: () -> Unit,
    onToggleRegistration: (() -> Unit)?
) {
    Card(
        onClick = onLaunch,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRegistered) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bmp = remember(app.key) { app.icon.toBitmap(96, 96).asImageBitmap() }
            Image(
                painter = BitmapPainter(bmp),
                contentDescription = app.label,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                app.label,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
            if (onToggleRegistration != null) {
                IconButton(
                    onClick = onToggleRegistration,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isRegistered) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isRegistered) "登録を解除" else "アプリを登録",
                        tint = if (isRegistered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun requestHome(context: Context) {
    if (android.os.Build.VERSION.SDK_INT >= 29) {
        val rm = context.getSystemService(RoleManager::class.java)
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
            context.startActivity(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
            return
        }
    }
    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
}
