# Reglas de R8 para FinanzasApp.
# Room, Compose, WorkManager y DataStore traen sus propias reglas.

# SQLCipher: la librería nativa llama a estas clases por JNI.
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**

# PDFBox-Android carga fuentes, glifos y filtros por nombre.
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.harmony.** { *; }
-dontwarn com.tom_roush.**
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn javax.xml.**

# Los mensajes de error y los nombres de clase ayudan a leer un fallo.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
