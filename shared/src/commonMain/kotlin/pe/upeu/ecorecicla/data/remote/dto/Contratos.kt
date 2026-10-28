package pe.upeu.ecorecicla.data.remote.dto
import kotlinx.serialization.Serializable
import pe.upeu.ecorecicla.domain.model.*
@Serializable data class LoginDto(val usuario: String,val contrasena: String)
@Serializable data class SesionDto(val token: String,val usuario: String,val nombre: String,val rol: String,val privilegios: Set<String>,val expiraEn: String) {
    fun dominio() = Sesion(token,usuario,rol,privilegios,nombre,expiraEn)
}
@Serializable data class UsuarioDto(val id: Long,val usuario: String,val nombre: String,val rol: String,val privilegios: Set<String>)
@Serializable data class PuntoDto(val id: Long,val razonSocial: String,val direccion: String,val zona: String,val materialHabitual: String,val secuencia: Int = 0,val atendido: Boolean = false,val motivoOmision: String? = null) {
    fun dominio() = PuntoAcopio(id,razonSocial,direccion,zona,materialHabitual,secuencia,atendido,motivoOmision)
}
@Serializable data class RutaDto(val id: Long,val fecha: String,val estado: String,val puntos: List<PuntoDto>)
@Serializable data class PesajeDto(val id: Long,val rutaId: Long,val puntoId: Long,val fechaHora: String,val material: String,val pesoBruto: Double,val tara: Double,val pesoNeto: Double,val operario: String,val estado: String,val motivoAnulacion: String? = null,val observacion: String = "",val humedadEstimada: Double = 0.0) {
    fun dominio() = Pesaje(id,puntoId,fechaHora,material,pesoBruto,tara,pesoNeto,operario,if(estado=="ANULADO") EstadoPesaje.Anulado(motivoAnulacion.orEmpty()) else EstadoPesaje.Registrado,false,rutaId,observacion,humedadEstimada=humedadEstimada)
}
@Serializable data class EscrituraPesajeDto(val solicitudId: String,val rutaId: Long,val puntoId: Long,val material: String,val pesoBruto: Double,val tara: Double,val observacion: String,val humedadEstimada: Double = 0.0)
fun FormularioPesaje.dto(solicitudId: String) = EscrituraPesajeDto(solicitudId,rutaId,puntoId,material,pesoBruto,tara,observacion,humedadEstimada)
@Serializable data class MotivoDto(val motivo: String)
@Serializable data class PaginaDto<T>(val contenido: List<T>,val pagina: Int,val tamanio: Int,val totalElementos: Long,val totalPaginas: Int,val ultima: Boolean)
@Serializable data class ErrorDto(val timestamp: String = "",val status: Int = 0,val error: String = "",val message: String = "Error del servicio",val path: String = "",val validationErrors: Map<String,String> = emptyMap())
