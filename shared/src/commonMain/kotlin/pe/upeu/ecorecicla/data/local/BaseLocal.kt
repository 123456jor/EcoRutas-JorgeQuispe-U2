package pe.upeu.ecorecicla.data.local
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import pe.upeu.ecorecicla.db.*
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.data.remote.dto.*
class BaseLocal(val database: EcoDatabase) {
    val q=database.ecoQueries
    fun rutas()=q.rutas().asFlow().mapToList(Dispatchers.Default)
    fun puntos(ruta: Long,busqueda: String)=q.puntos(ruta,busqueda).asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { PuntoAcopio(it.id,it.razonSocial,it.direccion,it.zona,it.materialHabitual,it.secuencia.toInt(),it.atendido,it.motivoOmision) } }
    fun pesajes(punto: Long)=q.pesajes(punto).asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.dominio() } }
    fun guardar(dto: PesajeDto) { val d=dto.dominio(); q.guardarPesaje(d.id,d.rutaId,d.puntoId,d.fechaHora,d.material,d.pesoBruto,d.tara,d.pesoNeto,d.operario,dto.estado,dto.motivoAnulacion,false,d.observacion,null,d.humedadEstimada) }
    fun limpiar()=database.transaction { q.borrarOperaciones(); q.borrarPesajes(); q.borrarPuntos(); q.borrarRutas() }
}
fun PesajeLocal.dominio()=Pesaje(id,puntoId,fechaHora,material,pesoBruto,tara,pesoNeto,operario,if(estado=="ANULADO") EstadoPesaje.Anulado(motivo.orEmpty()) else EstadoPesaje.Registrado,pendiente,rutaId,observacion,error,humedadEstimada)
