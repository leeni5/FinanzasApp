package com.finanzasapp.ui.screens.categorias

import com.finanzasapp.R
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.finanzasapp.data.entity.Category
import com.finanzasapp.ui.components.ColorPicker
import com.finanzasapp.ui.components.IconCatalog
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.IdentityPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.theme.asColor
import com.finanzasapp.ui.theme.readableInk
import com.finanzasapp.ui.theme.toHex

/** Cómo se pinta el icono de la categoría. */
private enum class IconInk(@StringRes val label: Int) { AUTO(R.string.fz_ink_auto), BLANCO(R.string.fz_ink_white), NEGRO(R.string.fz_ink_black), OTRO(R.string.fz_ink_pick) }

/**
 * Crear o editar una categoría: nombre, icono, color de fondo y color del
 * icono, con una vista previa que cambia en vivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorSheet(
    initial: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onArchive: ((Category) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val base = initial ?: Category(name = "", color = IdentityPalette.random().toHex())

    var name by remember { mutableStateOf(base.name) }
    var iconKey by remember { mutableStateOf(base.icon) }
    var color by remember { mutableStateOf(base.color.asColor()) }
    var iconInk by remember {
        mutableStateOf(
            when (base.iconColor.uppercase()) {
                "" -> IconInk.AUTO
                "#FFFFFF" -> IconInk.BLANCO
                "#000000" -> IconInk.NEGRO
                else -> IconInk.OTRO
            }
        )
    }
    var customIconColor by remember {
        mutableStateOf(base.iconColor.takeIf { it.isNotBlank() }?.asColor() ?: Color.White)
    }
    var showIconPicker by remember { mutableStateOf(false) }

    val iconColor: Color? = when (iconInk) {
        IconInk.AUTO -> null
        IconInk.BLANCO -> Color.White
        IconInk.NEGRO -> Color.Black
        IconInk.OTRO -> customIconColor
    }
    val animatedBg by animateColorAsState(color, tween(220), label = "previewBg")
    val canSave = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppPalette.SurfaceStrong) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Spacing.page)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text(
                text = if (initial == null) stringResource(R.string.fz_cat_new) else stringResource(R.string.fz_cat_edit),
                style = MaterialTheme.typography.headlineSmall,
                color = AppPalette.Ink
            )

            // Vista previa: así se verá en Movimientos.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(animatedBg)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = IconCatalog.forCategory(name, iconKey),
                    contentDescription = null,
                    tint = iconColor ?: animatedBg.readableInk(),
                    modifier = Modifier.size(30.dp)
                )
                Spacer(Modifier.width(Spacing.lg))
                Text(
                    text = name.ifBlank { stringResource(R.string.fz_cat_name_placeholder) },
                    style = MaterialTheme.typography.titleMedium,
                    color = animatedBg.readableInk(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TextField(
                value = name,
                onValueChange = { name = it.take(32) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.fz_name)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = editorFieldColors()
            )

            // Icono
            Surface(
                onClick = { showIconPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = AppPalette.SurfaceMuted
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = IconCatalog.forCategory(name, iconKey),
                        contentDescription = null,
                        tint = AppPalette.Ink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        text = if (iconKey.isBlank()) stringResource(R.string.fz_cat_choose_an_icon) else stringResource(R.string.fz_cat_change_icon),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.Ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(stringResource(R.string.fz_cat_see_all), style = MaterialTheme.typography.labelMedium, color = AppPalette.Accent)
                }
            }

            EditorLabel(stringResource(R.string.fz_color))
            ColorPicker(color = color, onColorChange = { color = it })

            EditorLabel(stringResource(R.string.fz_cat_icon_color))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                IconInk.entries.forEach { option ->
                    val selected = option == iconInk
                    Surface(
                        onClick = { iconInk = option },
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) AppPalette.Accent else AppPalette.SurfaceMuted
                    ) {
                        Text(
                            text = stringResource(option.label),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) AppPalette.OnAccent else AppPalette.InkSoft,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            AnimatedVisibility(visible = iconInk == IconInk.OTRO) {
                ColorPicker(color = customIconColor, onColorChange = { customIconColor = it })
            }

            Button(
                onClick = {
                    onSave(
                        base.copy(
                            name = name.trim(),
                            icon = iconKey,
                            color = color.toHex(),
                            iconColor = iconColor?.toHex() ?: ""
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppPalette.Accent,
                    contentColor = AppPalette.OnAccent,
                    disabledContainerColor = AppPalette.SurfaceStrong,
                    disabledContentColor = AppPalette.InkFaint
                )
            ) {
                Text(
                    text = if (initial == null) stringResource(R.string.fz_cat_create) else stringResource(R.string.fz_save_changes),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (initial != null && onArchive != null) {
                TextButton(
                    onClick = { onArchive(initial) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (initial.archived) stringResource(R.string.fz_cat_restore) else stringResource(R.string.fz_cat_archive),
                        color = AppPalette.Coral,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showIconPicker) {
        IconPickerDialog(
            selected = iconKey,
            tint = color,
            onDismiss = { showIconPicker = false },
            onPick = {
                iconKey = it
                showIconPicker = false
            }
        )
    }
}

/** Selector de iconos: buscador arriba y los iconos agrupados por tema. */
@Composable
fun IconPickerDialog(
    selected: String,
    tint: Color,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { IconCatalog.search(query) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xxl),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.fz_cat_choose_icon),
                        style = MaterialTheme.typography.titleLarge,
                        color = AppPalette.Ink,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(onClick = onDismiss, shape = CircleShape, color = AppPalette.SurfaceMuted) {
                        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, stringResource(R.string.fz_close), tint = AppPalette.InkSoft, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.fz_cat_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = AppPalette.InkSoft) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = editorFieldColors()
                )
                Spacer(Modifier.height(Spacing.md))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(52.dp),
                    contentPadding = PaddingValues(bottom = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    if (query.isBlank()) {
                        IconCatalog.groups.forEach { group ->
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = group.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AppPalette.InkSoft,
                                    modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs)
                                )
                            }
                            items(group.icons, key = { it.key }) { icon ->
                                IconCell(icon.vector, icon.key == selected, tint) { onPick(icon.key) }
                            }
                        }
                    } else if (results.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(R.string.fz_cat_no_icons, query),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppPalette.InkSoft
                            )
                        }
                    } else {
                        items(results, key = { it.key }) { icon ->
                            IconCell(icon.vector, icon.key == selected, tint) { onPick(icon.key) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconCell(
    vector: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) tint else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = vector,
            contentDescription = null,
            tint = if (selected) tint.readableInk() else AppPalette.Ink,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun EditorLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = AppPalette.InkSoft
    )
}

@Composable
private fun editorFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = AppPalette.SurfaceMuted,
    unfocusedContainerColor = AppPalette.SurfaceMuted,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = AppPalette.Accent,
    focusedLabelColor = AppPalette.Accent
)
