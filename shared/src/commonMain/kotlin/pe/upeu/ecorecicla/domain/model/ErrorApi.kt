package pe.upeu.ecorecicla.domain.model
sealed interface ErrorApi {
    val mensaje: String
    data class Validacion(val campos: Map<String,String>,override val mensaje: String): ErrorApi
    data class NoAutorizado(override val mensaje: String): ErrorApi
    data class SinPrivilegio(override val mensaje: String): ErrorApi
    data class NoEncontrado(override val mensaje: String): ErrorApi
    data class Conflicto(override val mensaje: String): ErrorApi
    data class Servidor(override val mensaje: String): ErrorApi
    data class SinConexion(override val mensaje: String = "Sin conexión. Tus datos locales siguen disponibles."): ErrorApi
    data class TiempoAgotado(override val mensaje: String = "La solicitud tardó demasiado. Intenta nuevamente."): ErrorApi
}
class ErrorApiException(val error: ErrorApi): Exception(error.mensaje)
