package com.satoya.novalauncher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val shown = apps.filter { query.isBlank() || it.label.contains(query, true) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Nova Launcher", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(query, { query = it }, label = { Text("アプリを検索") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { apps = repo.loadApps() }) { Text("更新") }
            Button(onClick = { requestHome(context) }) { Text("既定のホームに設定") }
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }) { Text("設定") }
        }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(columns = GridCells.Adaptive(88.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shown, key = { it.key }) { app ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { repo.launch(app) }.padding(6.dp)) {
                    val bmp = remember(app.icon) { app.icon.toBitmap(128,128).asImageBitmap() }
                    Image(BitmapPainter(bmp), app.label, Modifier.size(52.dp))
                    Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = {
                        repo.setFavorite(app, app.key !in favorites)
                        favorites = repo.favorites()
                    }) { Text(if (app.key in favorites) "★" else "☆") }
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
