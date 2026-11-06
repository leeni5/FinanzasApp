package com.finanzasapp.domain.payments

import com.finanzasapp.data.entity.PaymentFrequency
import com.finanzasapp.data.entity.ScheduledPayment
import java.time.LocalDate

/**
 * Cálculo del siguiente vencimiento de un pago programado.
 *
 * Usa el día ancla para no "encoger" los pagos: uno del 31 vence el 28/29 en
 * febrero y vuelve al 31 en marzo. Uno anual del 29 de febrero vence el 28
 * en años no bisiestos y vuelve al 29 cuando el año lo tiene.
 */
object PaymentCycle {

    /** Día ancla del pago: el guardado o, si falta, el de su vencimiento actual. */
    fun anchorOf(payment: ScheduledPayment): Int =
        (payment.anchorDay ?: payment.nextDueDate.dayOfMonth).coerceIn(1, 31)

    /** Siguiente vencimiento, o null si el pago es único (se desactiva). */
    fun next(payment: ScheduledPayment): LocalDate? {
        val anchor = anchorOf(payment)
        val due = payment.nextDueDate
        return when (payment.frequency) {
            PaymentFrequency.MENSUAL -> clampDay(due.plusMonths(1), anchor)
            PaymentFrequency.ANUAL -> clampDay(due.plusYears(1), anchor)
            PaymentFrequency.UNICA -> null
        }
    }

    /** Lleva una fecha al día ancla, o al último día del mes si no existe. */
    fun clampDay(date: LocalDate, anchor: Int): LocalDate =
        date.withDayOfMonth(minOf(anchor, date.lengthOfMonth()))
}
