package com.finanzasapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing

/**
 * Título de sección y, si hace falta, un dato o una acción a la derecha.
 * Si hay [onTrailing], el texto de la derecha se ve en el acento y se toca.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailing: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                color = if (onTrailing != null) AppPalette.Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (onTrailing != null) {
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onTrailing)
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                } else {
                    Modifier
                }
            )
        }
    }
}
