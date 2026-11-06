package com.finanzasapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.Normalizer

/** Un icono que se puede asignar a una categoría. */
data class CatalogIcon(
    val key: String,
    val vector: ImageVector,
    val keywords: String
)

/** Grupo del selector de iconos. */
data class IconGroup(val title: String, val icons: List<CatalogIcon>)

/**
 * Catálogo de iconos para categorías. La clave se guarda en `categories.icon`,
 * así que **no se renombra una clave existente**: las categorías la perderían.
 */
object IconCatalog {

    private fun i(key: String, vector: ImageVector, keywords: String) =
        CatalogIcon(key, vector, keywords)

    val groups: List<IconGroup> = listOf(
        IconGroup(
            "Hogar",
            listOf(
                i("home", Icons.Rounded.Home, "casa hogar arriendo vivienda"),
                i("apartment", Icons.Rounded.Apartment, "apartamento edificio administracion"),
                i("bed", Icons.Rounded.Bed, "cama cuarto habitacion"),
                i("chair", Icons.Rounded.Chair, "muebles silla sala"),
                i("bolt", Icons.Rounded.Bolt, "luz energia electricidad"),
                i("water", Icons.Rounded.WaterDrop, "agua acueducto"),
                i("fire", Icons.Rounded.LocalFireDepartment, "gas"),
                i("wifi", Icons.Rounded.Wifi, "internet wifi"),
                i("phone", Icons.Rounded.PhoneAndroid, "celular plan datos telefono"),
                i("cleaning", Icons.Rounded.CleaningServices, "aseo limpieza"),
                i("laundry", Icons.Rounded.LocalLaundryService, "lavanderia ropa"),
                i("build", Icons.Rounded.Build, "arreglos mantenimiento servicios"),
                i("handyman", Icons.Rounded.Handyman, "herramientas reparacion")
            )
        ),
        IconGroup(
            "Comida",
            listOf(
                i("cart", Icons.Rounded.ShoppingCart, "mercado supermercado compras"),
                i("grocery", Icons.Rounded.LocalGroceryStore, "tienda mercado"),
                i("restaurant", Icons.Rounded.Restaurant, "restaurante comida almuerzo"),
                i("fastfood", Icons.Rounded.Fastfood, "comida rapida hamburguesa domicilio"),
                i("pizza", Icons.Rounded.LocalPizza, "pizza"),
                i("cafe", Icons.Rounded.LocalCafe, "cafe tinto desayuno"),
                i("bar", Icons.Rounded.LocalBar, "bar trago cerveza"),
                i("cake", Icons.Rounded.Cake, "postre torta cumpleanos"),
                i("cookie", Icons.Rounded.Cookie, "galleta snack mecato"),
                i("icecream", Icons.Rounded.Icecream, "helado")
            )
        ),
        IconGroup(
            "Transporte",
            listOf(
                i("car", Icons.Rounded.DirectionsCar, "carro auto transporte"),
                i("bus", Icons.Rounded.DirectionsBus, "bus pasaje transmilenio"),
                i("taxi", Icons.Rounded.LocalTaxi, "taxi uber didi"),
                i("moto", Icons.Rounded.TwoWheeler, "moto"),
                i("gas", Icons.Rounded.LocalGasStation, "gasolina combustible"),
                i("parking", Icons.Rounded.LocalParking, "parqueadero"),
                i("flight", Icons.Rounded.Flight, "avion vuelo viaje"),
                i("train", Icons.Rounded.Train, "tren metro"),
                i("luggage", Icons.Rounded.Luggage, "viaje maleta"),
                i("hotel", Icons.Rounded.Hotel, "hotel hospedaje")
            )
        ),
        IconGroup(
            "Salud y personal",
            listOf(
                i("health", Icons.Rounded.MonitorHeart, "salud corazon"),
                i("hospital", Icons.Rounded.LocalHospital, "hospital clinica eps"),
                i("medication", Icons.Rounded.Medication, "medicina farmacia drogueria"),
                i("medical", Icons.Rounded.MedicalServices, "medico cita"),
                i("fitness", Icons.Rounded.FitnessCenter, "gimnasio deporte"),
                i("spa", Icons.Rounded.Spa, "spa belleza cuidado"),
                i("haircut", Icons.Rounded.ContentCut, "peluqueria corte"),
                i("clothes", Icons.Rounded.Checkroom, "ropa vestuario"),
                i("bag", Icons.Rounded.ShoppingBag, "compras bolsa tienda"),
                i("mall", Icons.Rounded.LocalMall, "centro comercial"),
                i("person", Icons.Rounded.Person, "personal yo"),
                i("face", Icons.Rounded.Face, "cositas personal")
            )
        ),
        IconGroup(
            "Familia y ocio",
            listOf(
                i("family", Icons.Rounded.FamilyRestroom, "familia"),
                i("child", Icons.Rounded.ChildCare, "hijos bebe ninos"),
                i("pets", Icons.Rounded.Pets, "mascota perro gato veterinario"),
                i("gift", Icons.Rounded.CardGiftcard, "regalo regalos"),
                i("celebration", Icons.Rounded.Celebration, "fiesta celebracion"),
                i("movie", Icons.Rounded.Movie, "cine pelicula"),
                i("tv", Icons.Rounded.Tv, "television streaming netflix"),
                i("music", Icons.Rounded.MusicNote, "musica spotify"),
                i("games", Icons.Rounded.SportsEsports, "videojuegos juegos"),
                i("soccer", Icons.Rounded.SportsSoccer, "futbol deporte"),
                i("park", Icons.Rounded.Park, "parque paseo"),
                i("beach", Icons.Rounded.BeachAccess, "playa vacaciones"),
                i("umbrella", Icons.Rounded.Umbrella, "sombrilla cuarto"),
                i("subscriptions", Icons.Rounded.Subscriptions, "suscripciones"),
                i("heart", Icons.Rounded.Favorite, "amor pareja favorito"),
                i("charity", Icons.Rounded.VolunteerActivism, "donacion caridad")
            )
        ),
        IconGroup(
            "Trabajo y estudio",
            listOf(
                i("school", Icons.Rounded.School, "estudio colegio universidad"),
                i("book", Icons.AutoMirrored.Rounded.MenuBook, "libros curso"),
                i("work", Icons.Rounded.Work, "trabajo oficina"),
                i("business", Icons.Rounded.Business, "negocio empresa"),
                i("computer", Icons.Rounded.Computer, "computador tecnologia"),
                i("idea", Icons.Rounded.Lightbulb, "idea proyecto"),
                i("public", Icons.Rounded.Public, "internet paquetes datos mundo")
            )
        ),
        IconGroup(
            "Dinero",
            listOf(
                i("salary", Icons.Rounded.AttachMoney, "salario sueldo nomina ingreso"),
                i("payments", Icons.Rounded.Payments, "pagos efectivo billetes"),
                i("savings", Icons.Rounded.Savings, "ahorro alcancia"),
                i("bank", Icons.Rounded.AccountBalance, "banco cuenta"),
                i("card", Icons.Rounded.CreditCard, "tarjeta credito deuda"),
                i("invest", Icons.AutoMirrored.Rounded.TrendingUp, "inversion rendimientos"),
                i("receipt", Icons.AutoMirrored.Rounded.ReceiptLong, "factura recibo impuestos"),
                i("coins", Icons.Rounded.Toll, "monedas"),
                i("wallet", Icons.Rounded.AccountBalanceWallet, "billetera"),
                i("award", Icons.Rounded.WorkspacePremium, "premio bono"),
                i("refund", Icons.Rounded.Replay, "devuelto devolucion reembolso")
            )
        ),
        IconGroup(
            "Otros",
            listOf(
                i("star", Icons.Rounded.Star, "favorito especial"),
                i("category", Icons.Rounded.Category, "categoria general"),
                i("more", Icons.Rounded.MoreHoriz, "otros varios otro")
            )
        )
    )

    private val all: List<CatalogIcon> = groups.flatMap { it.icons }
    private val byKey: Map<String, CatalogIcon> = all.associateBy { it.key }

    /** Busca por nombre o palabra clave, sin tildes ni mayúsculas. */
    fun search(query: String): List<CatalogIcon> {
        val q = normalize(query)
        if (q.isBlank()) return all
        return all.filter { normalize(it.keywords).contains(q) || it.key.contains(q) }
    }

    fun byKey(key: String): CatalogIcon? = byKey[key]

    /**
     * Icono de una categoría: el que eligió el usuario si existe; si no, uno
     * deducido del nombre (las categorías creadas por la semilla o la
     * importación no traen icono).
     */
    fun forCategory(name: String?, key: String?): ImageVector {
        key?.takeIf { it.isNotBlank() }?.let { byKey[it] }?.let { return it.vector }
        val n = normalize(name.orEmpty())
        if (n.isBlank()) return Icons.Rounded.Category
        return all.firstOrNull { icon -> normalize(icon.keywords).split(' ').any { it.length > 2 && n.contains(it) } }
            ?.vector ?: Icons.Rounded.Category
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .trim()
}
