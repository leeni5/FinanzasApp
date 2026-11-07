package com.finanzasapp.ui.screens.respaldo

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.finanzasapp.FinanzasApp
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.IconBubble
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Interruptor del bloqueo con huella. Activarlo pide la huella una vez. */
@Composable
fun LockCard() {
    val context = LocalContext.current
    val lock = remember(context) { (context.applicationContext as FinanzasApp).appLock }
    var enabled by remember { mutableStateOf(lock.enabled) }
    var message by remember { mutableStateOf<String?>(null) }

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon = Icons.Rounded.Fingerprint, tint = AppPalette.Violet, size = 44.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Bloqueo con huella", style = MaterialTheme.typography.titleMedium, color = AppPalette.Ink)
                Text(
                    "Pide tu huella (o el PIN del teléfono) al abrir la app y al volver " +
                        "a ella después de 30 segundos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.InkSoft
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Switch(
                checked = enabled,
                onCheckedChange = { wanted ->
                    val activity = context.findActivity() ?: return@Switch
                    val reason = lock.unavailableReason(context)
                    if (reason != null) {
                        message = reason
                        return@Switch
                    }
                    // Tanto para activar como para desactivar se confirma con la huella.
                    lock.authenticate(activity, if (wanted) "Activar el bloqueo" else "Quitar el bloqueo") { ok, error ->
                        if (ok) {
                            lock.enabled = wanted
                            enabled = wanted
                            message = null
                        } else if (error != null) {
                            message = error
                        }
                    }
                }
            )
        }
        message?.let {
            Spacer(Modifier.height(Spacing.sm))
            Text(it, style = MaterialTheme.typography.bodySmall, color = AppPalette.Coral)
        }
    }
}

/**
 * Guarda una copia del instalador (APK) de esta misma versión, para pasarla a
 * otro teléfono o reinstalar. Se copia el APK que Android tiene instalado.
 */
@Composable
fun ShareApkCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val source = File(context.applicationInfo.sourceDir)
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { out ->
                        source.inputStream().use { it.copyTo(out) }
                    }
                }.isSuccess
            }
            Toast.makeText(
                context,
                if (ok) "Instalador guardado." else "No se pudo guardar el instalador.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon = Icons.Rounded.Android, tint = AppPalette.Teal, size = 44.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Descargar la app (APK)", style = MaterialTheme.typography.titleMedium, color = AppPalette.Ink)
                Text(
                    "Comparte el instalador de esta versión ($version) por WhatsApp, Drive o correo, " +
                        "o guárdalo en el teléfono. No lleva tus datos: quien lo instale empieza con la app vacía.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.InkSoft
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(
                onClick = { scope.launch { shareApk(context, version) } },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent),
                contentPadding = PaddingValues(vertical = Spacing.md)
            ) { Text("Compartir") }
            OutlinedButton(
                onClick = { launcher.launch("FinanzasApp-$version.apk") },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(vertical = Spacing.md)
            ) { Text("Guardar", color = AppPalette.Ink) }
        }
    }
}

/**
 * Copia el APK instalado a la caché y abre el menú de compartir de Android.
 * Solo esa copia queda expuesta, por el FileProvider, y solo para lectura.
 */
private suspend fun shareApk(context: Context, version: String) {
    val file = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "compartir").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            File(dir, "FinanzasApp-$version.apk").also { target ->
                File(context.applicationInfo.sourceDir).copyTo(target, overwrite = true)
            }
        }.getOrNull()
    }
    if (file == null) {
        Toast.makeText(context, "No se pudo preparar el instalador.", Toast.LENGTH_LONG).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("application/vnd.android.package-archive")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, "FinanzasApp $version: instálala abriendo el archivo (Android pedirá permitir apps de esta fuente).")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, "Compartir FinanzasApp").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
