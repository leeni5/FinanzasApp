package com.finanzasapp.ui.screens.metas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.withTransaction
import com.finanzasapp.data.db.AppDatabase
import com.finanzasapp.data.dao.GoalDao
import com.finanzasapp.data.entity.AuditLog
import com.finanzasapp.data.entity.GoalContribution
import com.finanzasapp.data.entity.SavingsGoal
import com.finanzasapp.data.model.GoalProgress
import com.finanzasapp.ui.util.Fechas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Lectura de una meta frente a su fecha y al ritmo real de aportes. */
enum class GoalStatus {
    COMPLETADA,
    EN_RITMO,
    ATRASADA,
    VENCIDA,
    SIN_RITMO
}

/** Aporte visible en el detalle de una meta. */
data class ContributionRow(
    val id: Long,
    val date: LocalDate,
    val amount: Long
)

/** Meta con todo lo que la pantalla necesita para explicarla sin consultar la base. */
data class GoalRow(
    val goalId: Long,
    val name: String,
    val targetAmount: Long,
    val targetDate: LocalDate,
    val color: String,
    val saved: Long,
    val remaining: Long,
    val progress: Float,
    val completed: Boolean,
    val status: GoalStatus,
    val recentMonthly: Long,
    val requiredMonthly: Long,
    val projectedDate: LocalDate?,
    val contributions: List<ContributionRow>
)

data class MetasUiState(
    val active: List<GoalRow> = emptyList(),
    val completed: List<GoalRow> = emptyList(),
    val totalSaved: Long = 0L,
    val totalTarget: Long = 0L,
    val progress: Float = 0f,
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val loading: Boolean = true
) {
    val goals: List<GoalRow> get() = active + completed
}

/** Borrador del editor: sirve para crear y para editar una meta. */
data class GoalDraft(
    val id: Long? = null,
    val name: String = "",
    val targetAmount: Long = 0L,
    val targetDate: LocalDate = Fechas.hoy().plusMonths(6),
    val color: String = "#4C5BD4",
    val saved: Long = 0L,
    val completed: Boolean = false
)

/**
 * Estado de la pantalla de metas.
 *
 * El ahorrado nunca se escribe a mano: es la suma de los aportes. El ritmo se
 * calcula con los aportes de los últimos tres meses y la proyección usa ese
 * ritmo para decir en qué mes se llegaría a la meta.
 */
class MetasViewModel(private val database: AppDatabase) : ViewModel() {

    private val today = Fechas.hoy()

    val state: StateFlow<MetasUiState> = combine(
        database.goalDao().observeProgress(),
        database.goalDao().observeAllContributions()
    ) { goals, contributions ->
        buildState(goals, contributions)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MetasUiState()
    )

    fun saveGoal(draft: GoalDraft) {
        val name = draft.name.trim()
        if (name.isBlank() || draft.targetAmount <= 0L) return

        viewModelScope.launch {
            database.withTransaction {
                val dao = database.goalDao()
                val now = Instant.now()

                if (draft.id == null) {
                    val goal = SavingsGoal(
                        name = name,
                        targetAmount = draft.targetAmount,
                        targetDate = draft.targetDate,
                        color = draft.color
                    )
                    val id = dao.insertGoal(goal)
                    database.systemDao().insertAudit(
                        AuditLog(
                            entity = "goal",
                            entityId = id,
                            action = "CREADO",
                            beforeJson = null,
                            afterJson = goal.toAuditJson(),
                            source = "usuario",
                            at = now
                        )
                    )
                } else {
                    val before = dao.findGoal(draft.id) ?: return@withTransaction
                    val saved = dao.total(before.id)
                    val after = before.copy(
                        name = name,
                        targetAmount = draft.targetAmount,
                        targetDate = draft.targetDate,
                        color = draft.color,
                        completed = saved >= draft.targetAmount
                    )
                    if (after == before) return@withTransaction

                    dao.updateGoal(after)
                    database.systemDao().insertAudit(
                        AuditLog(
                            entity = "goal",
                            entityId = after.id,
                            action = "EDITADO",
                            beforeJson = before.toAuditJson(),
                            afterJson = after.toAuditJson(),
                            source = "usuario",
                            at = now
                        )
                    )
                }
            }
        }
    }

    fun addContribution(goalId: Long, date: LocalDate, amount: Long) {
        if (amount <= 0L) return

        viewModelScope.launch {
            database.withTransaction {
                val dao = database.goalDao()
                val goal = dao.findGoal(goalId) ?: return@withTransaction
                val contribution = GoalContribution(
                    goalId = goalId,
                    date = date,
                    amount = amount
                )
                val id = dao.insertContribution(contribution)
                database.systemDao().insertAudit(
                    AuditLog(
                        entity = "goal_contribution",
                        entityId = id,
                        action = "CREADO",
                        beforeJson = null,
                        afterJson = contribution.toAuditJson(goal.name),
                        source = "usuario",
                        at = Instant.now()
                    )
                )
                syncCompletion(dao, goal)
            }
        }
    }

    fun deleteContribution(goalId: Long, contribution: ContributionRow) {
        viewModelScope.launch {
            database.withTransaction {
                val dao = database.goalDao()
                val goal = dao.findGoal(goalId) ?: return@withTransaction
                val entity = GoalContribution(
                    id = contribution.id,
                    goalId = goalId,
                    date = contribution.date,
                    amount = contribution.amount
                )
                dao.deleteContribution(entity)
                database.systemDao().insertAudit(
                    AuditLog(
                        entity = "goal_contribution",
                        entityId = contribution.id,
                        action = "BORRADO",
                        beforeJson = entity.toAuditJson(goal.name),
                        afterJson = null,
                        source = "usuario",
                        at = Instant.now()
                    )
                )
                syncCompletion(dao, goal)
            }
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            database.withTransaction {
                val dao = database.goalDao()
                val goal = dao.findGoal(goalId) ?: return@withTransaction
                dao.deleteGoal(goal)
                database.systemDao().insertAudit(
                    AuditLog(
                        entity = "goal",
                        entityId = goal.id,
                        action = "BORRADO",
                        beforeJson = goal.toAuditJson(),
                        afterJson = null,
                        source = "usuario",
                        at = Instant.now()
                    )
                )
            }
        }
    }

    /**
     * La meta guarda una bandera de completada para ordenar y para Inicio.
     * Se recalcula después de cada aporte, nunca se edita a mano.
     */
    private suspend fun syncCompletion(dao: GoalDao, goal: SavingsGoal) {
        val total = dao.total(goal.id)
        val completed = total >= goal.targetAmount
        if (goal.completed != completed) {
            dao.updateGoal(goal.copy(completed = completed))
        }
    }

    private fun buildState(
        goals: List<GoalProgress>,
        contributions: List<GoalContribution>
    ): MetasUiState {
        val byGoal = contributions.groupBy { it.goalId }
        val currentMonth = YearMonth.from(today)

        val rows = goals.map { progress ->
            val list = byGoal[progress.goalId].orEmpty().sortedByDescending { it.date }
            val completed = progress.completed ||
                progress.targetAmount <= 0L ||
                progress.saved >= progress.targetAmount
            val remaining = (progress.targetAmount - progress.saved).coerceAtLeast(0L)
            val fraction = if (progress.targetAmount > 0L) {
                progress.saved.toFloat() / progress.targetAmount
            } else {
                1f
            }
            val recentMonthly = monthlyRhythm(list)
            val monthsLeft = ChronoUnit.MONTHS
                .between(currentMonth, YearMonth.from(progress.targetDate))
                .coerceAtLeast(1L)
            val requiredMonthly = if (remaining > 0L) {
                remaining / monthsLeft
            } else {
                0L
            }
            val status = statusFor(
                completed = completed,
                targetDate = progress.targetDate,
                recentMonthly = recentMonthly,
                requiredMonthly = requiredMonthly
            )

            GoalRow(
                goalId = progress.goalId,
                name = progress.name,
                targetAmount = progress.targetAmount,
                targetDate = progress.targetDate,
                color = progress.color,
                saved = progress.saved,
                remaining = remaining,
                progress = fraction.coerceIn(0f, 1f),
                completed = completed,
                status = status,
                recentMonthly = recentMonthly,
                requiredMonthly = requiredMonthly,
                projectedDate = projectionDate(remaining, recentMonthly),
                contributions = list.map { contribution ->
                    ContributionRow(
                        id = contribution.id,
                        date = contribution.date,
                        amount = contribution.amount
                    )
                }
            )
        }

        val active = rows
            .filterNot { it.completed }
            .sortedWith(
                compareBy<GoalRow> { statusWeight(it.status) }
                    .thenBy { it.targetDate }
            )
        val completed = rows
            .filter { it.completed }
            .sortedBy { it.targetDate }

        val totalSaved = rows.sumOf { it.saved }
        val totalTarget = rows.sumOf { it.targetAmount }

        return MetasUiState(
            active = active,
            completed = completed,
            totalSaved = totalSaved,
            totalTarget = totalTarget,
            progress = if (totalTarget > 0L) {
                (totalSaved.toFloat() / totalTarget).coerceIn(0f, 1f)
            } else {
                0f
            },
            activeCount = active.size,
            completedCount = completed.size,
            loading = false
        )
    }

    /**
     * Promedio mensual de los últimos tres meses calendario.
     * Se divide entre los meses transcurridos desde el primer aporte (máximo 3)
     * para no castigar una meta que apenas empezó.
     */
    private fun monthlyRhythm(contributions: List<GoalContribution>): Long {
        if (contributions.isEmpty()) return 0L

        val currentMonth = YearMonth.from(today)
        val firstMonth = YearMonth.from(contributions.minOf { it.date })
        val monthsTracked = ChronoUnit.MONTHS
            .between(firstMonth, currentMonth)
            .coerceIn(1L, 3L)
        val windowStart = today.minusMonths(2).withDayOfMonth(1)

        val inWindow = contributions
            .filter { !it.date.isBefore(windowStart) && !it.date.isAfter(today) }
            .sumOf { it.amount }

        return inWindow / monthsTracked
    }

    private fun statusFor(
        completed: Boolean,
        targetDate: LocalDate,
        recentMonthly: Long,
        requiredMonthly: Long
    ): GoalStatus = when {
        completed -> GoalStatus.COMPLETADA
        targetDate.isBefore(today) -> GoalStatus.VENCIDA
        recentMonthly <= 0L -> GoalStatus.SIN_RITMO
        recentMonthly >= requiredMonthly -> GoalStatus.EN_RITMO
        else -> GoalStatus.ATRASADA
    }

    private fun projectionDate(remaining: Long, monthly: Long): LocalDate? = when {
        remaining <= 0L -> today
        monthly <= 0L -> null
        else -> {
            val months = (remaining + monthly - 1) / monthly
            if (months > 600L) null else today.plusMonths(months)
        }
    }

    private fun statusWeight(status: GoalStatus): Int = when (status) {
        GoalStatus.VENCIDA -> 0
        GoalStatus.ATRASADA -> 1
        GoalStatus.SIN_RITMO -> 2
        GoalStatus.EN_RITMO -> 3
        GoalStatus.COMPLETADA -> 4
    }

    private fun SavingsGoal.toAuditJson(): String = JSONObject()
        .put("nombre", name)
        .put("objetivo", targetAmount)
        .put("fecha", targetDate.toString())
        .put("color", color)
        .put("completada", completed)
        .toString()

    private fun GoalContribution.toAuditJson(goalName: String): String = JSONObject()
        .put("meta", goalName)
        .put("fecha", date.toString())
        .put("monto", amount)
        .toString()

    companion object {
        fun factory(database: AppDatabase) = viewModelFactory {
            initializer { MetasViewModel(database) }
        }
    }
}
