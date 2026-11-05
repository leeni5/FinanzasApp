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
import com.finanzasapp.ui.components.AppCard
import com.finanzasapp.ui.components.ColorDot
import com.finanzasapp.ui.components.EmptyState
import com.finanzasapp.ui.components.ScreenHeader
import com.finanzasapp.ui.theme.AppPalette
import com.finanzasapp.ui.theme.Spacing
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
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
                ScreenHeader(
                    title = "Movimientos",
                    subtitle = "Tus ingresos y gastos, en orden."
                )
            }

            item {
                MonthCard(
                    month = state.month,
                    isCurrentMonth = state.isCurrentMonth,
                    canGoForward = state.canGoForward,
                    income = state.income,
                    expenses = state.expenses,
                    monthCount = state.monthCount,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    SearchField(
                        query = state.query,
                        onQueryChange = viewModel::setQuery
                    )
                    FilterTabs(
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
                        message = "Registra tu primer movimiento y aquí verás el detalle " +
                            "del día, la categoría y la cuenta.",
                        icon = Icons.Filled.Add,
                        actionLabel = "Registrar el primero",
                        onAction = {
                            editor = newMovement(activeCategories, activeAccounts)
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

        FloatingActionButton(
            onClick = { editor = newMovement(activeCategories, activeAccounts) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.page, bottom = Spacing.lg),
            shape = CircleShape,
            containerColor = AppPalette.Indigo,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Registrar movimiento",
                modifier = Modifier.size(24.dp)
            )
        }
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
            }
        )
    }
}

// ------------------------------------------------------------------- mes

@Composable
private fun MonthCard(
    month: YearMonth,
    isCurrentMonth: Boolean,
    canGoForward: Boolean,
    income: Long,
    expenses: Long,
    monthCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    AppCard(contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Mes anterior",
                    tint = AppPalette.InkSoft,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Fechas.mesDe(month.atDay(1)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isCurrentMonth) "Mes actual" else "$monthCount movimientos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onNext, enabled = canGoForward) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Mes siguiente",
                    tint = if (canGoForward) AppPalette.InkSoft else AppPalette.InkFaint.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        Row {
            MonthTotal(
                label = "Ingresos",
                amount = income,
                color = AppPalette.Green,
                modifier = Modifier.weight(1f)
            )
            MonthTotal(
                label = "Gastos",
                amount = expenses,
                color = AppPalette.Coral,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthTotal(
    label: String,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = Money.cop(amount),
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
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
private fun FilterTabs(
    selected: MovementFilter,
    onSelect: (MovementFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        MovementFilter.entries.forEach { filter ->
            val isSelected = filter == selected

            Column(
                modifier = Modifier
                    .clickable { onSelect(filter) }
                    .padding(top = Spacing.xs, bottom = Spacing.xs)
            ) {
                Text(
                    text = filter.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AppPalette.Indigo else Color.Transparent)
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xs, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayLabel(group.date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (group.rows.size == 1) "1 movimiento" else "${group.rows.size} movimientos",
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.InkFaint
            )
        }

        group.rows.forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
            MovementRowItem(
                row = row,
                onClick = { onEdit(row) }
            )
        }
    }
}

@Composable
private fun MovementRowItem(
    row: MovementRow,
    onClick: () -> Unit
) {
    val transaction = row.transaction
    val dotColor = when (transaction.type) {
        TransactionType.INGRESO -> AppPalette.Green
        TransactionType.GASTO -> AppPalette.InkFaint
        TransactionType.TRANSFERENCIA -> AppPalette.Sky
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorDot(color = dotColor, size = 8.dp)
        Spacer(Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.displayName(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(row.category?.name ?: "Sin categoría")
                    row.account?.let {
                        append("  ·  ")
                        append(it.name)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = amountLabel(transaction),
            style = MaterialTheme.typography.labelLarge,
            color = when (transaction.type) {
                TransactionType.INGRESO -> AppPalette.Green
                TransactionType.GASTO -> MaterialTheme.colorScheme.onSurface
                TransactionType.TRANSFERENCIA -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

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
            color = AppPalette.Indigo
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovementEditorSheet(
    initial: MovementDraft,
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (MovementDraft) -> Unit,
    onDelete: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember(initial) { mutableStateOf(initial.type) }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (initial.id == null) "Nuevo movimiento" else "Editar movimiento",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Se guarda solo en este teléfono.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = AppPalette.SurfaceMuted
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
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

            TypeSelector(
                type = type,
                onGasto = {
                    type = TransactionType.GASTO
                    if (categoryId == incomeCategoryId) {
                        categoryId = expenseCategoryId
                    }
                },
                onIngreso = {
                    type = TransactionType.INGRESO
                    if (incomeCategoryId != null) {
                        categoryId = incomeCategoryId
                    }
                }
            )

            Spacer(Modifier.height(Spacing.lg))

            FieldLabel("Monto")
            TextField(
                value = amountText,
                onValueChange = { value ->
                    amountText = value.filter { it.isDigit() }.take(12)
                },
                modifier = Modifier.fillMaxWidth(),
                prefix = {
                    Text(
                        text = "\$",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppPalette.InkFaint
                    )
                },
                placeholder = {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppPalette.InkFaint
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
                colors = filledFieldColors()
            )

            Spacer(Modifier.height(Spacing.md))

            FieldLabel("Descripción")
            TextField(
                value = description,
                onValueChange = { description = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Ej. Almuerzo, taxi, mercado…",
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
            if (categories.isEmpty()) {
                Text(
                    text = "Todavía no hay categorías.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(categories, key = { it.id }) { category ->
                        ChoiceChip(
                            label = category.name,
                            selected = category.id == categoryId,
                            onClick = { categoryId = category.id }
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            FieldLabel("Cuenta")
            if (accounts.isEmpty()) {
                Text(
                    text = "Necesitas una cuenta para registrar movimientos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.Coral
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(accounts, key = { it.id }) { account ->
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
                    Text(
                        text = "Cambiar",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppPalette.Indigo
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))

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
                    .height(52.dp),
                enabled = canSave,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppPalette.Indigo,
                    contentColor = Color.White,
                    disabledContainerColor = AppPalette.SurfaceStrong,
                    disabledContentColor = AppPalette.InkFaint
                )
            ) {
                Text(
                    text = if (initial.id == null) "Registrar movimiento" else "Guardar cambios",
                    style = MaterialTheme.typography.labelLarge
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
                        text = "Eliminar movimiento",
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
private fun TypeSelector(
    type: TransactionType,
    onGasto: () -> Unit,
    onIngreso: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AppPalette.SurfaceMuted
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            TypeSegment(
                label = "Gasto",
                selected = type == TransactionType.GASTO,
                onClick = onGasto,
                modifier = Modifier.weight(1f)
            )
            TypeSegment(
                label = "Ingreso",
                selected = type == TransactionType.INGRESO,
                onClick = onIngreso,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TypeSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
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
        shape = RoundedCornerShape(50),
        color = if (selected) AppPalette.Ink else AppPalette.SurfaceMuted
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else AppPalette.InkSoft,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
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
    cursorColor = AppPalette.Indigo,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)

// --------------------------------------------------------------- helpers

private fun newMovement(
    categories: List<Category>,
    accounts: List<Account>
): MovementDraft {
    val incomeId = categories.firstOrNull { it.name.equals("Ingresos", ignoreCase = true) }?.id
    return MovementDraft(
        type = TransactionType.GASTO,
        categoryId = categories.firstOrNull { it.id != incomeId }?.id ?: categories.firstOrNull()?.id,
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
