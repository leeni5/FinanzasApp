package com.finanzasapp.data.entity

/** Tipo de cuenta. Define qué cálculos y qué etiquetas se muestran. */
enum class AccountType {
    DEBITO,
    CREDITO,
    EFECTIVO,
    AHORRO
}

/** Naturaleza del movimiento. */
enum class TransactionType {
    GASTO,
    INGRESO,
    TRANSFERENCIA
}

/** Cómo llegó el movimiento a la app. Se guarda siempre, es parte de la trazabilidad. */
enum class TransactionSource {
    MANUAL,
    RAPIDA,
    CSV,
    PDF,
    IA
}

enum class PaymentFrequency {
    MENSUAL,
    ANUAL,
    UNICA
}

enum class ImportKind {
    CSV,
    XLSX,
    PDF
}

enum class ImportStatus {
    PENDIENTE,
    PROCESANDO,
    REVISAR,
    COMPLETADO,
    ERROR
}
