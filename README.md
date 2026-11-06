# FinanzasApp

App Android personal para llevar ingresos, gastos, presupuestos, metas y pagos
programados. Todo vive en el teléfono: no hay backend, no hay cuenta de usuario y
no se leen notificaciones bancarias.

## Estado

- **Fase 0 completa:** base de datos Room, tema, navegación y datos iniciales.
- **Fase 1 completa:** movimientos por mes con búsqueda y filtros, alta, edición y
  borrado con confirmación. Cada cambio queda registrado en un historial de auditoría.
- **Fase 2 completa:** presupuestos por categoría con navegación mensual, gasto
  acumulado, disponible, ritmo diario y avisos cuando una categoría se excede o
  va camino de agotar su límite. Los cambios de límite también se auditan.
- **Fase 3 completa:** metas con aportes reales, avance calculado, ritmo de los
  últimos tres meses, proyección de llegada y estados de en ritmo, atrasada,
  vencida o completada. Los aportes y cambios de meta se auditan.
- **Fase 4 completa:** importación de CSV, Excel y del PDF exportado por Budge.
  Primero se muestra una vista previa; nada se escribe hasta confirmar. Las
  categorías y cuentas desconocidas se concilian, y volver a importar el mismo
  archivo no duplica movimientos.
- **Interfaz:** tipografía Inter suave, fondo neutro, tarjetas blancas sin bordes,
  un solo acento y color reservado para el dinero y las alertas.
- **Seguridad:** base de datos cifrada con SQLCipher y clave protegida por el
  Android Keystore.

## Stack

- Kotlin + Jetpack Compose + Material 3
- Room 2.8.5 con esquema exportado
- DataStore Preferences
- WorkManager (fase de pagos programados)
- SQLCipher 4.20.0 para cifrado en reposo
- PDFBox-Android para leer el PDF sin salir del teléfono
- minSdk 26, targetSdk 36, Java 17

## Importación

- **CSV y Excel:** se leen por encabezados (`Fecha`, `Descripción`, `Monto`,
  `Categoría`, `Cuenta`). Si no hay encabezados, se usa el orden del export de
  Budge como respaldo.
- **PDF:** se acepta el export de Budge. El texto se extrae en el propio teléfono
  y se interpreta por filas; las descripciones largas partidas en varias líneas
  se vuelven a unir.
- **Vista previa:** muestra cuántos movimientos se detectan, los totales, las
  categorías que se crearán, las cuentas que se conciliaron, la cuenta para las
  filas sin cuenta y una muestra de los primeros movimientos.
- **Duplicados:** cada fila recibe una huella única. Al reimportar el mismo
  archivo, las filas ya registradas se marcan como "Ya existe" y se omiten.
- Todo el proceso queda registrado en el historial de importaciones de Ajustes.

## Seguridad de los datos

- La base `finanzas.db` se cifra completa con **SQLCipher (AES-256)**. Sin la
  clave, el archivo no se puede abrir ni inspeccionar con SQLite.
- La frase de la base son **32 bytes aleatorios**. Se guarda envuelta con
  **AES-256/GCM** usando una llave que vive en el **Android Keystore** y que no
  se puede exportar.
- `allowBackup="false"` y reglas de extracción que excluyen todos los dominios:
  ni Google Drive ni la transferencia entre teléfonos copian la base.
- La app **no pide permiso de INTERNET**. Cuando se active Gemini (fase 7) se
  añadirá solo para esa función.
- En la versión final, `FLAG_SECURE` bloquea capturas y la vista reciente. En
  depuración se deja libre para poder revisar el diseño.
- No hay nube, ni APIs bancarias, ni lectura de notificaciones, ni multiusuario.

## Datos de ejemplo

Las compilaciones de depuración siembran cuentas, categorías, movimientos de
cuatro meses, pagos y una meta para poder probar la interfaz. La versión release
arranca vacía: solo crea las categorías base y la cuenta "Efectivo".

## Compilar

Requisitos: JDK 17 o superior y Android SDK 36 con build-tools 36.0.0.

```bash
# local.properties (no se versiona)
sdk.dir=/ruta/al/Android/Sdk

./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Instalación

Al instalar un APK de depuración por ADB, algunos antivirus de terceros pueden
marcarlo como sospechoso por usar una firma de desarrollo. Es un falso positivo:
elige "Cancelar/Permitir" o añade una exclusión. Para distribuir, usa una
compilación release firmada con tu propia llave.

## Fases siguientes

- **Fase 5:** pagos programados con notificaciones locales.
- **Fase 7:** resúmenes con Gemini.
