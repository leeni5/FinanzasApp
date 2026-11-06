package com.finanzasapp.domain.importing

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

/**
 * Extrae el texto del PDF en el propio teléfono con PDFBox-Android.
 * Después, BudgeTextParser convierte ese texto en movimientos.
 */
object PdfParser {

    @Volatile
    private var initialized = false

    fun parse(context: Context, bytes: ByteArray): List<ParsedRow> {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    initialized = true
                }
            }
        }

        val text = try {
            PDDocument.load(bytes).use { document ->
                if (document.isEncrypted) {
                    throw ImportParseException(
                        "El PDF está protegido con contraseña. " +
                            "Expórtalo sin contraseña e inténtalo de nuevo."
                    )
                }
                PDFTextStripper().getText(document)
            }
        } catch (error: ImportParseException) {
            throw error
        } catch (error: Exception) {
            throw ImportParseException(
                "No pudimos leer el PDF. Verifica que no esté dañado ni protegido."
            )
        }

        return BudgeTextParser.parse(text)
    }
}
