package com.finanzasapp.ui.screens.movimientos

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finanzasapp.FinanzasApp
import com.finanzasapp.data.entity.Account
import com.finanzasapp.data.entity.Category
import com.finanzasapp.data.entity.Transaction
import com.finanzasapp.data.entity.TransactionType
import com.finanzasapp.data.model.displayName
import androidx.compose.foundation.lazy.rememberLazyListState
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.AnimatedMoney
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberCoroutineScope
import com.finanzasapp.ui.screens.categorias.CategoryActions
import com.finanzasapp.ui.screens.categorias.CategoryEditorSheet
import com.finanzasapp.ui.theme.soft
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import com.finanzasapp.ui.components.SpeedDialAction
import com.finanzasapp.ui.components.SpeedDialFab
import com.finanzasapp.ui.components.CategoryBadge
import com.finanzasapp.ui.components.IconCatalog
import com.finanzasapp.ui.components.pressScale
import com.finanzasapp.ui.theme.readableInk
import com.finanzasapp.ui.theme.readableInkSoft
import com.finanzasapp.ui.components.HeroSurface
import com.finanzasapp.ui.components.ColorDot
import com.finanzasapp.ui.components.EmptyState
import com.finanzasapp.ui.components.ScreenHeader
import com.finanzasapp.ui.components.ScrollAwareFab
import com.finanzasapp.ui.components.SoftPill
import com.finanzasapp.ui.components.TintedCard
import com.finanzasapp.ui.components.isScrollingUp
import com.finanzasapp.ui.theme.soft
import com.finanzasapp.ui.screens.importar.ImportSheet
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
import com.finanzasapp.ui.theme.asColor
import com.finanzasapp.ui.theme.identityColor
import com.finanzasapp.ui.util.Fechas
import com.finanzasapp.ui.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@Composable
fun MovimientosScreen() {
    val context = LocalContext.current
    val database = remember(context) {
        (context.applicationContext as FinanzasApp).database
    }
    val viewModel: MovimientosViewModel = viewModel(
        factory = MovimientosViewModel.factory(database)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val activeCategories = remember(state.categories) {
        state.categories.filter { !it.archived }
    }
    val activeAccounts = remember(state.accounts) {
        state.accounts.filter { !it.archived }
    }

    var editor by remember { mutableStateOf<MovementDraft?>(null) }
    var dialOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showImport by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val fabVisible = listState.isScrollingUp()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.page,
                end = Spacing.page,
                top = Spacing.xl,
                bottom = 108.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenHeader(
                        title = "Movimientos",
                        subtitle = "Tus ingresos y gastos, en orden.",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Surface(
                        onClick = { showImport = true },
                        shape = MaterialTheme.shapes.medium,
                        color = AppPalette.AccentSoft
                    ) {
                        Text(
                            text = "Importar",
                            style = MaterialTheme.typography.labelLarge,
                            color = AppPalette.Accent,
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 9.dp
                            )
                        )
                    }
                }
            }

            item {
                BalanceHero(
                    month = state.month,
                    isCurrentMonth = state.isCurrentMonth,
                    canGoForward = state.canGoForward,
                    balance = state.income - state.expenses,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    SummaryCard(
                        label = "Ingresos",
                        amount = state.income,
                        color = AppPalette.Green,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = "Gastos",
                        amount = state.expenses,
                        color = AppPalette.Coral,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    SearchField(
                        query = state.query,
                        onQueryChange = viewModel::setQuery
                    )
                    FilterPills(
                        selected = state.filter,
                        onSelect = viewModel::setFilter
                    )
                }
            }

            when {
                state.loading -> item { LoadingRow() }

                state.monthCount == 0 -> item {
                    EmptyState(
                        title = "Este mes está en blanco",
                        message = "Registra tu primer movimiento o importa el archivo " +
                            "que ya tienes. Aquí verás el detalle del día, la categoría " +
                            "y la cuenta.",
                        icon = Icons.Filled.Add,
                        actionLabel = "Registrar el primero",
                        onAction = {
                            dialOpen = true
                        }
                    )
                }

                state.visibleCount == 0 -> item {
                    EmptyState(
                        title = "Sin resultados",
                        message = "No encontramos movimientos para “${state.query}”. " +
                            "Prueba con otra palabra o quita el filtro.",
                        icon = Icons.Filled.Search,
                        actionLabel = "Limpiar búsqueda",
                        onAction = {
                            viewModel.setQuery("")
                            viewModel.setFilter(MovementFilter.TODOS)
                        }
                    )
                }

                else -> items(
                    items = state.groups,
                    key = { group -> group.date.toEpochDay() }
                ) { group ->
                    DayGroup(
                        group = group,
                        onEdit = { row -> editor = row.toDraft() }
                    )
                }
            }
        }

        // «+» despliega Gasto e Ingreso: el tipo se decide antes de abrir el
        // formulario, que así queda enfocado en una sola cosa.
        val expenseColor = AppPalette.Coral
        val incomeColor = AppPalette.Green
        SpeedDialFab(
            expanded = dialOpen,
            onExpandedChange = { dialOpen = it },
            visible = fabVisible,
            actions = listOf(
                SpeedDialAction("Ingreso", Icons.AutoMirrored.Rounded.TrendingUp, incomeColor) {
                    editor = newMovement(activeCategories, activeAccounts, TransactionType.INGRESO)
                },
                SpeedDialAction("Gasto", Icons.AutoMirrored.Rounded.TrendingDown, expenseColor) {
                    editor = newMovement(activeCategories, activeAccounts, TransactionType.GASTO)
                }
            )
        )
    }

    editor?.let { draft ->
        MovementEditorSheet(
            initial = draft,
            categories = activeCategories,
            accounts = activeAccounts,
            onDismiss = { editor = null },
            onSave = { savedDraft ->
                viewModel.save(savedDraft)
                editor = null
            },
            onDelete = { id ->
                viewModel.delete(id)
                editor = null
            },
            onCreateCategory = { category, onCreated ->
                scope.launch { onCreated(CategoryActions.save(database, category)) }
            }
        )
    }

    if (showImport) {
        ImportSheet(
            onDismiss = { showImport = false },
            onImported = { date ->
                date?.let(viewModel::showMonth)
                showImport = false
            }
        )
    }
}

// ------------------------------------------------------------------- mes

/**
 * Tarjeta de saldo: una superficie oscura y plana que da contexto al mes.
 * El saldo es lo único grande; los totales viven debajo, en tarjetas blancas.
 */
@Composable
private fun BalanceHero(
    month: YearMonth,
    isCurrentMonth: Boolean,
    canGoForward: Boolean,
    balance: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    HeroSurface(contentPadding = PaddingValues(Spacing.lg)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonthArrow(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    description = "Mes anterior",
                    enabled = true,
                    onClick = onPrevious
                )
                Text(
                    text = Fechas.mesDe(month.atDay(1)),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppPalette.HeroInk,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                MonthArrow(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    description = "Mes siguiente",
                    enabled = canGoForward,
                    onClick = onNext
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = "Saldo del mes",
                style = MaterialTheme.typography.labelMedium,
                color = AppPalette.HeroInkSoft
            )
            Spacer(Modifier.height(Spacing.xxs))
            AnimatedMoney(amount = balance, style = MaterialTheme.typography.displaySmall, color = AppPalette.HeroInk)

            Spacer(Modifier.height(Spacing.md))

            SoftPill(
                text = if (isCurrentMonth) "Mes actual" else "Mes cerrado",
                color = AppPalette.HeroInk,
                background = AppPalette.HeroChip
            )
        }
    }
}

@Composable
private fun MonthArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) AppPalette.HeroInkSoft else AppPalette.HeroLine,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    TintedCard(
        modifier = modifier,
        color = color.soft(0.10f),
        contentPadding = PaddingValues(Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(color = color, size = 8.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        AnimatedMoney(amount = amount, style = MaterialTheme.typography.titleLarge, color = color)
    }
}

// ------------------------------------------------------- búsqueda y filtros

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Buscar movimiento",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Limpiar búsqueda",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = filledFieldColors()
    )
}

@Composable
private fun FilterPills(
    selected: MovementFilter,
    onSelect: (MovementFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        MovementFilter.entries.forEach { filter ->
            val isSelected = filter == selected

            Surface(
                onClick = { onSelect(filter) },
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) AppPalette.Accent else AppPalette.SurfaceMuted
            ) {
                Text(
                    text = filter.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) AppPalette.OnAccent else AppPalette.InkSoft,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------- lista

@Composable
private fun DayGroup(
    group: MovementGroup,
    onEdit: (MovementRow) -> Unit
) {
    val total = group.rows.sumOf { row ->
        if (row.transaction.type == TransactionType.INGRESO) {
            row.transaction.amount
        } else {
            -row.transaction.amount
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        // Encabezado del día, fuera de las tarjetas.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm, bottom = Spacing.xxs),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = dayLabel(group.date),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = Fechas.diaSemana(group.date),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.InkFaint,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = amountForTotal(total),
                style = MaterialTheme.typography.labelMedium,
                color = if (total >= 0L) AppPalette.Green else AppPalette.InkSoft
            )
        }

        group.rows.forEach { row ->
            MovementRowItem(
                row = row,
                onClick = { onEdit(row) }
            )
        }
    }
}

/**
 * Un movimiento: tarjeta llena del color de su categoría, con su icono, el
 * nombre, la cuenta y el monto. El texto se vuelve claro u oscuro según lo
 * claro que sea el color, para que siempre se lea.
 */
@Composable
private fun MovementRowItem(
    row: MovementRow,
    onClick: () -> Unit
) {
    val transaction = row.transaction
    val category = row.category
    val background = category?.color?.asColor() ?: AppPalette.SurfaceMuted
    val ink = if (category != null) background.readableInk() else AppPalette.Ink
    val inkSoft = if (category != null) background.readableInkSoft() else AppPalette.InkSoft
    val iconTint = category?.iconColor?.takeIf { it.isNotBlank() }?.asColor() ?: ink
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = IconCatalog.forCategory(category?.name, category?.icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.width(Spacing.lg))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.displayName(),
                style = MaterialTheme.typography.titleSmall,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = row.account?.name ?: (category?.name ?: "Sin categoría"),
                style = MaterialTheme.typography.bodySmall,
                color = inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = amountLabel(transaction),
            style = MaterialTheme.typography.titleSmall,
            color = ink
        )
    }
}

private fun amountForTotal(total: Long): String =
    if (total > 0L) "+ ${Money.cop(total)}" else Money.cop(total)

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = AppPalette.Accent
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = "Leyendo tus movimientos…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -------------------------------------------------------------- editor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MovementEditorSheet(
    initial: MovementDraft,
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (MovementDraft) -> Unit,
    onDelete: (Long) -> Unit,
    onCreateCategory: (Category, (Long) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val type = initial.type
    var amountText by remember(initial) {
        mutableStateOf(if (initial.amount > 0L) initial.amount.toString() else "")
    }
    var description by remember(initial) { mutableStateOf(initial.description) }
    var date by remember(initial) { mutableStateOf(initial.date) }

    val incomeCategoryId = remember(categories) {
        categories.firstOrNull { it.name.equals("Ingresos", ignoreCase = true) }?.id
    }
    val expenseCategoryId = remember(categories, incomeCategoryId) {
        categories.firstOrNull { it.id != incomeCategoryId }?.id
    }

    var categoryId by remember(initial) { mutableStateOf(initial.categoryId) }
    var accountId by remember(initial) { mutableStateOf(initial.accountId) }

    LaunchedEffect(categories, accounts) {
        if (categoryId == null) {
            categoryId = if (type == TransactionType.INGRESO) {
                incomeCategoryId ?: expenseCategoryId
            } else {
                expenseCategoryId
            }
        }
        if (accountId == null) {
            accountId = accounts.firstOrNull()?.id
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showNewCategory by remember { mutableStateOf(false) }
    // El tipo llega decidido desde el botón «+» (o desde el movimiento editado).
    val isIncome = type == TransactionType.INGRESO
    val typeColor = if (isIncome) AppPalette.Green else AppPalette.Coral

    val amount = amountText.toLongOrNull() ?: 0L
    val canSave = amount > 0L &&
        description.isNotBlank() &&
        accountId != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = AppPalette.Border)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Spacing.page)
                .padding(top = Spacing.xs, bottom = Spacing.xxl)
        ) {
            // 1. Encabezado: qué se está registrando, con el color del tipo.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(typeColor.soft()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            initial.id != null && isIncome -> "Editar ingreso"
                            initial.id != null -> "Editar gasto"
                            isIncome -> "Nuevo ingreso"
                            else -> "Nuevo gasto"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Se guarda solo en este teléfono.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(onClick = onDismiss, shape = CircleShape, color = AppPalette.SurfaceMuted) {
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = AppPalette.InkSoft,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            // 2. El monto, protagonista: grande, centrado y del color del tipo.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(typeColor.soft(0.10f))
                    .padding(vertical = Spacing.lg, horizontal = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Monto",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppPalette.InkSoft
                )
                TextField(
                    value = amountText,
                    onValueChange = { value -> amountText = value.filter { it.isDigit() }.take(12) },
                visualTransformation = com.finanzasapp.ui.components.ThousandsTransformation,
                    modifier = Modifier.fillMaxWidth(),
                    prefix = {
                        Text("\$ ", style = MaterialTheme.typography.displaySmall, color = typeColor)
                    },
                    placeholder = {
                        Text(
                            "0",
                            style = MaterialTheme.typography.displaySmall,
                            color = AppPalette.InkFaint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        color = typeColor,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = typeColor
                    )
                )
                if (amount > 0L) {
                    Text(
                        text = (if (isIncome) "+ " else "− ") + Money.cop(amount),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPalette.InkSoft
                    )
                }
            }

            // 3. ¿Qué fue?
            EditorSection("¿Qué fue?")
            TextField(
                value = description,
                onValueChange = { description = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = if (isIncome) "Ej. Salario, venta, reembolso…" else "Ej. Almuerzo, taxi, mercado…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = filledFieldColors()
            )

            Spacer(Modifier.height(Spacing.md))
            FieldLabel("Categoría")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                categories.forEach { category ->
                    CategoryChip(
                        category = category,
                        selected = category.id == categoryId,
                        onClick = { categoryId = category.id }
                    )
                }
                ChoiceChip(
                    label = if (categories.isEmpty()) "+ Crear categoría" else "+ Nueva",
                    selected = false,
                    onClick = { showNewCategory = true }
                )
            }

            // 4. ¿De dónde y cuándo?
            EditorSection("¿De dónde y cuándo?")
            FieldLabel("Cuenta")
            if (accounts.isEmpty()) {
                Text(
                    text = "Necesitas una cuenta para registrar movimientos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.Coral
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    accounts.forEach { account ->
                        ChoiceChip(
                            label = account.name,
                            selected = account.id == accountId,
                            onClick = { accountId = account.id }
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))
            FieldLabel("Fecha")
            Surface(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = AppPalette.SurfaceMuted
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = AppPalette.InkSoft,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        text = Fechas.fechaLarga(date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text("Cambiar", style = MaterialTheme.typography.labelMedium, color = AppPalette.Accent)
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            // 5. Acción principal, en el color del tipo.
            Button(
                onClick = {
                    onSave(
                        MovementDraft(
                            id = initial.id,
                            type = type,
                            amount = amount,
                            description = description.trim(),
                            categoryId = categoryId,
                            accountId = accountId,
                            date = date
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = canSave,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = typeColor,
                    contentColor = typeColor.readableInk(),
                    disabledContainerColor = AppPalette.SurfaceStrong,
                    disabledContentColor = AppPalette.InkFaint
                )
            ) {
                Text(
                    text = when {
                        initial.id != null -> "Guardar cambios"
                        isIncome -> "Registrar ingreso"
                        else -> "Registrar gasto"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (!canSave) {
                Text(
                    text = when {
                        amount <= 0L -> "Escribe el monto para continuar."
                        description.isBlank() -> "Escribe qué fue para continuar."
                        else -> "Elige una cuenta para continuar."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.InkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm)
                )
            }

            if (initial.id != null) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = AppPalette.Coral)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = if (isIncome) "Eliminar ingreso" else "Eliminar gasto",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Listo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    if (showNewCategory) {
        CategoryEditorSheet(
            initial = null,
            onDismiss = { showNewCategory = false },
            onSave = { category ->
                showNewCategory = false
                onCreateCategory(category) { newId -> categoryId = newId }
            }
        )
    }

    if (showDeleteConfirm && initial.id != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar movimiento?") },
            text = {
                Text(
                    "El movimiento desaparece de la lista, pero la operación queda " +
                        "en el historial de la app."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(initial.id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppPalette.Coral)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (selected) AppPalette.Accent else AppPalette.SurfaceMuted
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AppPalette.OnAccent else AppPalette.InkSoft,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * Ficha de categoría para el formulario: con su color e icono. La elegida se
 * ve llena; las demás, con su color suave.
 */
@Composable
private fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit
) {
    val base = category.color.asColor()
    val bg by animateColorAsState(if (selected) base else base.soft(), tween(180), label = "chipBg")
    val ink = if (selected) base.readableInk() else AppPalette.Ink
    val iconTint = if (selected) {
        category.iconColor.takeIf { it.isNotBlank() }?.asColor() ?: ink
    } else {
        base
    }
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = IconCatalog.forCategory(category.name, category.icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(category.name, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}

/** Título de un bloque del formulario: separa las preguntas con aire. */
@Composable
private fun EditorSection(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = Spacing.xl, bottom = Spacing.md)
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Spacing.xs)
    )
}

@Composable
private fun filledFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = AppPalette.SurfaceMuted,
    unfocusedContainerColor = AppPalette.SurfaceMuted,
    disabledContainerColor = AppPalette.SurfaceMuted,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = AppPalette.Accent,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)

// --------------------------------------------------------------- helpers

private fun newMovement(
    categories: List<Category>,
    accounts: List<Account>,
    type: TransactionType = TransactionType.GASTO
): MovementDraft {
    val incomeId = categories.firstOrNull { it.name.equals("Ingresos", ignoreCase = true) }?.id
    val categoryId = if (type == TransactionType.INGRESO) {
        incomeId
    } else {
        categories.firstOrNull { it.id != incomeId }?.id ?: categories.firstOrNull()?.id
    }
    return MovementDraft(
        type = type,
        categoryId = categoryId,
        accountId = accounts.firstOrNull()?.id,
        date = Fechas.hoy()
    )
}

private fun MovementRow.toDraft(): MovementDraft = MovementDraft(
    id = transaction.id,
    type = transaction.type,
    amount = transaction.amount,
    description = transaction.displayName(),
    categoryId = transaction.categoryId,
    accountId = transaction.accountId,
    date = transaction.date
)

private fun amountLabel(transaction: Transaction): String = when (transaction.type) {
    TransactionType.INGRESO -> "+ ${Money.cop(transaction.amount)}"
    TransactionType.GASTO -> "− ${Money.cop(transaction.amount)}"
    TransactionType.TRANSFERENCIA -> Money.cop(transaction.amount)
}

private fun dayLabel(date: LocalDate): String {
    val today = Fechas.hoy()
    return when (date) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> Fechas.diaYMes(date)
    }
}
