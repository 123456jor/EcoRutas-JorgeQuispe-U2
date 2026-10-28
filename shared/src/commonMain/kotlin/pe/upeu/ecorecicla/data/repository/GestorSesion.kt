package pe.upeu.ecorecicla.data.repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import pe.upeu.ecorecicla.data.local.*
import pe.upeu.ecorecicla.data.remote.dto.SesionDto
class GestorSesion(private val almacen: AlmacenSeguro,private val plataforma: Plataforma) {
    private val estado=MutableStateFlow<SesionDto?>(null)
    val sesion=estado.asStateFlow()
    var alLimpiar: () -> Unit = {}
    fun restaurar() {
        val saved=runCatching { almacen.leer()?.let { Json.decodeFromString<SesionDto>(it) } }.getOrNull()
        if(saved != null && plataforma.tokenVigente(saved.token)) estado.value=saved else limpiar()
    }
    fun guardar(dto: SesionDto) { almacen.guardar(Json.encodeToString(dto)); estado.value=dto }
    fun token(): String? = estado.value?.token
    fun limpiar() { almacen.borrar(); alLimpiar(); estado.value=null }
}
