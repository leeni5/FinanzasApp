package com.finanzasapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finanzasapp.R
import com.finanzasapp.ui.components.IconBubble
import androidx.compose.ui.res.stringResource
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing

/**
 * Cortina opaca mientras la app está bloqueada: no deja ver ni tocar nada.
 * Pide la huella sola al aparecer; el botón es para reintentar si se canceló.
 */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    // Atrás no salta el bloqueo.
    BackHandler { }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .padding(Spacing.page),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconBubble(icon = Icons.Rounded.Fingerprint, tint = AppPalette.Accent, size = 72.dp)
        Spacer(Modifier.height(Spacing.lg))
        Text(stringResource(R.string.aj_lockscreen_title), style = MaterialTheme.typography.titleLarge, color = AppPalette.Ink)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.aj_lockscreen_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.InkSoft
        )
        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = onUnlock,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Accent, contentColor = AppPalette.OnAccent),
            contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md)
        ) { Text(stringResource(R.string.aj_lockscreen_unlock)) }
    }
}
