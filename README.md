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
- **Fase 5 completa:** pagos programados (mensual, anual o una vez) con
  vencidos, próximos y pausados; «Marcar como pagado» crea un único gasto en una
  sola transacción, avanza la fecha respetando el día ancla (un pago del 31
  vuelve al 31 después de febrero) y se puede deshacer. Avisos locales a 7, 2 y 0
  días a las 9:00 de Bogotá con WorkManager; sin permiso de notificaciones la
  app funciona igual.
- **Fase 6 completa:** administración de **categorías** (nombre, icono del
  catálogo, color de fondo y de icono, archivar) y de **cuentas** (entidad con
  logo, tipo, saldo inicial o cupo, color, archivar), con detalle por cuenta:
  saldo, entradas y salidas, torta de gastos por categoría y movimientos con
  filtro. Además:
  - **Copia de seguridad cifrada** con contraseña del usuario y restauración
    que reemplaza todo en una sola transacción; **exportación a CSV**.
  - **Transferencias** entre cuentas propias (botón «+» → Transferencia):
    dos movimientos unidos que mueven saldo sin contar como gasto ni ingreso.
  - **Revisión** (Ajustes → Revisión): bandeja «Por revisar», **reglas de
    categorización** e **historial de cambios** visible.
- **Fase 7 completa (sin probar con una clave real):** **chat con IA** para
  preguntar sobre las propias finanzas (Movimientos → «Preguntar» o Ajustes →
  «Abrir el chat»). Funciona con Groq, Mistral, OpenRouter o Gemini y la clave
  del usuario; ver «IA» más abajo.
- **Interfaz:** modo oscuro por defecto (selector Oscuro / Claro / Según el
  teléfono en Ajustes), acento violeta, barra inferior flotante con indicador
  animado, movimientos en tarjetas del color de su categoría, botón «+» que
  despliega Gasto o Ingreso, montos con separador de miles al escribir.
  Las alertas no dependen solo del color: llevan texto e icono.
- **Seguridad:** base de datos cifrada con SQLCipher y clave protegida por el
  Android Keystore.

## Stack

- Kotlin + Jetpack Compose + Material 3
- Room 2.8.5 con esquema exportado (versión 4; migraciones 1→2, 2→3 y 3→4 en
  `AppDatabase`)
- DataStore Preferences (tema)
- WorkManager 2.12.0 (avisos de pagos programados)
- Material Icons Extended (catálogo de iconos de categorías; R8 quita los no usados)
- SQLCipher 4.20.0 para cifrado en reposo
- PDFBox-Android para leer el PDF sin salir del teléfono
- minSdk 26, targetSdk 36, Java 17

## Copia de seguridad y exportación

- La base está cifrada con una llave del Keystore que no sale del teléfono y
  Android no la respalda: sin una copia, desinstalar la app o perder el teléfono
  borra los datos.
- **Crear copia** (Ajustes → Tus datos): todas las tablas en JSON, comprimidas y
  cifradas con **AES-256-GCM**; la llave sale de una contraseña del usuario
  (mínimo 8 caracteres) con **PBKDF2-SHA256, 210.000 vueltas** y sal aleatoria.
  Archivo `.finz`: `FINZ` + versión + sal + IV + texto cifrado. La app no guarda
  la contraseña: si se olvida, la copia no se abre.
- **Restaurar**: pide la contraseña, muestra qué trae la copia y, al confirmar,
  **reemplaza todo** en una transacción (si algo falla, la base queda igual).
  Solo acepta copias de la misma versión del esquema (hoy v4). Después
  reprograma los avisos de pago.
- **Exportar a CSV**: Fecha, Tipo, Descripción, Monto (negativo en gastos),
  Categoría, Cuenta y Notas; se puede volver a importar. Va sin cifrar.
- Los archivos se guardan con el selector del sistema: no hay permisos de
  almacenamiento.

## Transferencias

- Dos movimientos `TRANSFERENCIA` con el mismo `transferGroupId`: el que sale
  (`transferOut = 1`) resta saldo y el que entra lo suma.
- No cuentan en ingresos, gastos, presupuestos ni en el total del día. En el
  detalle de una cuenta sí aparecen como entrada o salida.
- Pagar la tarjeta desde el banco es una transferencia: baja la deuda.
- No se editan: se borran (las dos patas a la vez) y se crean de nuevo.

## Revisión, reglas e historial

- **Por revisar:** llegan los movimientos importados **sin categoría** o
  marcados como **no pagados**. Al confirmar uno se puede cambiar su
  categoría y crear una regla con su texto. También «Confirmar todos».
- **Reglas:** «si la descripción contiene X → categoría Y». Comparan sin
  mayúsculas, tildes ni signos. Al crearlas se aplican a los movimientos que
  hoy no tienen categoría; en cada importación rellenan solo las filas que
  llegan sin categoría. Nunca cambian una categoría ya elegida.
- **Historial:** los últimos 200 cambios del `audit_log` con quién los hizo
  (tú, importación, regla, pago programado). No se edita ni se borra.

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
- El permiso **INTERNET** existe solo para el chat con IA: sin una clave
  configurada la app no se conecta a nada.
- El único permiso en ejecución es **POST_NOTIFICATIONS** (Android 13+), para los
  avisos de pagos. Se pide una vez al abrir; si se niega, Ajustes ofrece activarlo.
- En la versión final, `FLAG_SECURE` bloquea capturas y la vista reciente. En
  depuración se deja libre para poder revisar el diseño.
- No hay nube, ni APIs bancarias, ni lectura de notificaciones, ni multiusuario.

## IA (chat)

- **Proveedores:** Groq, Mistral, OpenRouter y Gemini, todos con su API
  compatible con OpenAI (`/chat/completions` y `/models`), por
  `HttpURLConnection` y sin librerías nuevas. Se eligió así porque la API de
  Gemini pedía datos de facturación; Mistral confirma en su ayuda un plan
  gratuito sin tarjeta (con teléfono verificado), y Groq y OpenRouter tienen
  modelos gratuitos.
- **Modelos:** no están fijados en el código; al comprobar la clave se piden al
  proveedor (`/models`) y el usuario elige uno.
- **Clave:** cifrada con AES-256/GCM y una llave propia del Android Keystore
  (`finanzas_ia_key_v1`), en preferencias privadas. No va en las copias de
  seguridad de la app. Se puede borrar desde la misma hoja.
- **Qué se envía:** instrucciones fijas, la conversación y un texto con saldos
  por cuenta, totales, gasto por categoría con presupuesto y, si el interruptor
  está activo, cada movimiento del período (fecha, tipo, descripción, monto,
  categoría, cuenta; máximo 800). Nunca notas, metas, ids ni la base. El texto
  exacto se ve con el ojo de la pantalla del chat. No hay aviso antes de
  enviar: el usuario lo pidió así.
- **Período:** se deduce de la pregunta en el propio teléfono
  (`PeriodDetector`, sin red): «hoy», «ayer», «esta semana», «la semana
  pasada», «este mes», «el mes pasado», «octubre», «marzo de 2025», «de agosto
  a octubre», «los últimos 3 meses», «2025», «este año», «el año pasado». Solo
  se envían los movimientos de ese lapso (máximo 800). Si la pregunta no
  nombra fechas se usa el de la pregunta anterior; en la primera, los últimos
  12 meses.
- **Respuesta:** llega en streaming (`stream: true`) y se va mostrando mientras
  se escribe; antes, una burbuja con puntos animados dice en qué paso va. El
  botón de enviar se vuelve «Detener». En Gemini se pide `reasoning_effort:
  low` para que piense menos antes de responder (si el modelo no lo acepta, se
  reintenta sin él). Negritas y viñetas del markdown se muestran con formato.
- **Límites:** la IA solo responde texto; no crea ni cambia movimientos. La
  conversación no se guarda. Cada consulta queda en el historial (proveedor,
  modelo, período y cuántos movimientos se enviaron).
- **Errores:** sin red, clave inválida (401/403), modelo inexistente (404),
  límite del plan (429), demasiados datos (413) y respuesta lenta (90 s) se
  explican en el chat sin bloquear la app; «Cancelar» corta la conexión.
- **Privacidad:** en planes gratuitos el proveedor puede usar lo enviado para
  entrenar. Mistral permite desactivarlo en su consola.

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

## Logos de entidades

Los logos de `res/drawable-nodpi/logo_*.png` vienen de Wikimedia Commons,
recortados a su símbolo. Se cargan por nombre (`logo_<clave>`), así que agregar
una entidad es sumar su PNG y su línea en `BankCatalog`. El de Nequi tiene
licencia CC BY-SA 4.0 y su crédito aparece en Ajustes → Créditos; si se cambia o
se quita, hay que actualizar ese texto. Davivienda no tiene símbolo en Commons y
usa su insignia de color.

## Fases siguientes

- **Fase 7 (pendiente):** probar el chat con una clave real de cada proveedor.
- **Release:** keystore propio, R8 y QA final.
