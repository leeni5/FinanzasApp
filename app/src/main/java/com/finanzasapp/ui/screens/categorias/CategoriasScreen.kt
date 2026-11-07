package com.finanzasapp.ui.screens.categorias

import com.finanzasapp.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finanzasapp.FinanzasApp
import com.finanzasapp.data.entity.Category
import com.finanzasapp.ui.components.IconCatalog
import com.finanzasapp.ui.components.ScrollAwareFab
import com.finanzasapp.ui.components.SectionHeader
import com.finanzasapp.ui.components.isScrollingUp
import com.finanzasapp.ui.components.pressScale
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.theme.asColor
import com.finanzasapp.ui.theme.readableInk
import kotlinx.coroutines.launch

/** Administración de categorías: crear, editar, archivar y recuperar. */
@Composable
fun CategoriasScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember(context) { (context.applicationContext as FinanzasApp).database }
    val categories by database.categoryDao().observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    // null = cerrado; Category(id = 0) nunca se usa: la nueva se marca aparte.
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val active = categories.filter { !it.archived }
    val archived = categories.filter { it.archived }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.page, end = Spacing.page, top = Spacing.lg, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = onBack, shape = CircleShape, color = AppPalette.SurfaceMuted) {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.fz_back), tint = AppPalette.Ink)
                        }
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Text(stringResource(R.string.fz_cat_title), style = MaterialTheme.typography.headlineMedium, color = AppPalette.Ink)
                }
                Spacer(Modifier.padding(top = Spacing.sm))
                Text(
                    text = stringResource(R.string.fz_cat_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPalette.InkSoft
                )
                Spacer(Modifier.padding(top = Spacing.sm))
            }

            items(active, key = { it.id }) { category ->
                CategoryRowCard(category) { editing = category }
            }

            if (archived.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.fz_cat_archived),
                        trailing = "${archived.size}",
                        modifier = Modifier.padding(top = Spacing.lg)
                    )
                }
                items(archived, key = { it.id }) { category ->
                    CategoryRowCard(category, dimmed = true) { editing = category }
                }
            }
        }

        ScrollAwareFab(
            visible = listState.isScrollingUp(),
            description = stringResource(R.string.fz_cat_new),
            onClick = { creating = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.page, bottom = Spacing.lg)
        )
    }

    if (creating || editing != null) {
        CategoryEditorSheet(
            initial = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { category ->
                scope.launch { CategoryActions.save(database, category) }
                creating = false
                editing = null
            },
            onArchive = { category ->
                scope.launch { CategoryActions.setArchived(database, category.id, !category.archived) }
                editing = null
            }
        )
    }
}

/** Fila de categoría llena de su color, como se ve en Movimientos. */
@Composable
fun CategoryRowCard(
    category: Category,
    dimmed: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val background = category.color.asColor()
    val ink = background.readableInk()
    val iconTint = category.iconColor.takeIf { it.isNotBlank() }?.asColor() ?: ink
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.55f else 1f)
            .pressScale(interaction)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md + Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = IconCatalog.forCategory(category.name, category.icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(Spacing.lg))
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleSmall,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ink)
            )
        }
    }
}
