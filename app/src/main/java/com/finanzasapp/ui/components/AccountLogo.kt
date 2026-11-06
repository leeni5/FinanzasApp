package com.finanzasapp.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finanzasapp.data.entity.AccountType
import com.finanzasapp.ui.theme.asColor
import com.finanzasapp.ui.theme.readableInk
import java.text.Normalizer

/** Una entidad financiera conocida. */
data class Bank(
    val key: String,
    val name: String,
    /** Color de marca aproximado: fondo de la inicial si falta el logo. */
    val color: Color,
    val keywords: List<String>
)

/**
 * Entidades con logo incluido en la app (Wikimedia Commons; créditos en Ajustes). La clave se guarda en `accounts.logo`,
 * así que **no se renombra una clave existente**.
 *
 * El logo de cada una es el recurso `drawable/logo_<clave>` si existe; si no,
 * se dibuja la inicial sobre su color.
 */
object BankCatalog {
    val banks: List<Bank> = listOf(
        Bank("nequi", "Nequi", Color(0xFFDA0081), listOf("nequi")),
        Bank("bancolombia", "Bancolombia", Color(0xFFFDDA24), listOf("bancolombia")),
        Bank("davivienda", "Davivienda", Color(0xFFED1C24), listOf("davivienda")),
        Bank("bancodebogota", "Banco de Bogotá", Color(0xFF0B3B8C), listOf("bogota")),
        Bank("bbva", "BBVA", Color(0xFF004481), listOf("bbva")),
        Bank("nu", "Nu", Color(0xFF820AD1), listOf("nu ", "nubank", "nu colombia")),
        Bank("occidente", "Banco de Occidente", Color(0xFF0066B3), listOf("occidente")),
        Bank("colpatria", "Scotiabank Colpatria", Color(0xFFEC111A), listOf("colpatria", "scotiabank")),
        Bank("cajasocial", "Banco Caja Social", Color(0xFF005BAA), listOf("caja social")),
        Bank("avvillas", "AV Villas", Color(0xFF00529B), listOf("av villas", "avvillas")),
        Bank("popular", "Banco Popular", Color(0xFF00843D), listOf("popular")),
        Bank("falabella", "Banco Falabella", Color(0xFF3B8C2E), listOf("falabella")),
        Bank("paypal", "PayPal", Color(0xFF003087), listOf("paypal"))
    )

    private val byKey = banks.associateBy { it.key }

    fun byKey(key: String?): Bank? = key?.let { byKey[it] }

    /** Reconoce la entidad por el nombre de la cuenta ("Nequi Juan" → Nequi). */
    fun guess(name: String): Bank? {
        val n = " " + normalize(name) + " "
        return banks.firstOrNull { bank -> bank.keywords.any { n.contains(" ${normalize(it).trim()}") } }
    }

    fun resolve(logo: String?, name: String): Bank? = byKey(logo?.takeIf { it.isNotBlank() }) ?: guess(name)

    private fun normalize(text: String) = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
}

/**
 * Logo de una cuenta: el de su entidad si está en la app; si no, su inicial
 * sobre el color de la entidad (o el de la cuenta). El efectivo usa un icono.
 */
@SuppressLint("DiscouragedApi") // el recurso se busca por nombre para que el catálogo crezca sin tocar código
@Composable
fun AccountLogo(
    name: String,
    logo: String?,
    type: AccountType,
    color: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val context = LocalContext.current
    val bank = remember(logo, name) { BankCatalog.resolve(logo, name) }
    val resId = remember(bank) {
        bank?.let { context.resources.getIdentifier("logo_${it.key}", "drawable", context.packageName) } ?: 0
    }
    val shape = RoundedCornerShape(size * 0.28f)

    when {
        resId != 0 -> Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(Color.White)
                .padding(size * 0.08f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(resId),
                contentDescription = bank?.name,
                contentScale = ContentScale.Fit
            )
        }

        bank != null || type != AccountType.EFECTIVO -> {
            val bg = bank?.color ?: color.asColor()
            Box(
                modifier = modifier.size(size).clip(shape).background(bg),
                contentAlignment = Alignment.Center
            ) {
                if (bank != null) {
                    Text(
                        text = bank.name.first().uppercase(),
                        color = bg.readableInk(),
                        fontSize = (size.value * 0.42f).sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = when (type) {
                            AccountType.CREDITO -> Icons.Rounded.CreditCard
                            AccountType.AHORRO -> Icons.Rounded.Savings
                            else -> Icons.Rounded.AccountBalance
                        },
                        contentDescription = null,
                        tint = bg.readableInk(),
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }
        }

        else -> {
            val bg = color.asColor(Color(0xFF3DD68C))
            Box(
                modifier = modifier.size(size).clip(shape).background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Payments, null, tint = bg.readableInk(), modifier = Modifier.size(size * 0.55f))
            }
        }
    }
}
