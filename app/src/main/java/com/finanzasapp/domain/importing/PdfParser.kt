package com.finanzasapp.domain.importing

import android.content.Context
import com.finanzasapp.data.settings.Idioma
import com.finanzasapp.R
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
                        Idioma.text(R.string.mv_parse_pdf_password)
                    )
                }
                PDFTextStripper().getText(document)
            }
        } catch (error: ImportParseException) {
            throw error
        } catch (error: Exception) {
            throw ImportParseException(
                Idioma.text(R.string.mv_parse_pdf_unreadable)
            )
        }

        return BudgeTextParser.parse(text)
    }
}
