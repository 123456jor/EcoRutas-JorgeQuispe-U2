package pe.upeu.ecorecicla.domain.repository
import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.*
data class ResumenSync(val enviados: Int,val recibidos: Int,val errores: List<String>,val campos: Map<String,String> = emptyMap(),val ids: Map<Long,Long> = emptyMap())
interface EcoRepositorio {
    fun observarRuta(): Flow<Ruta?>
    fun observarPuntos(ruta: Long,busqueda: String): Flow<List<PuntoAcopio>>
    fun observarPesajes(punto: Long): Flow<List<Pesaje>>
    suspend fun guardar(p: FormularioPesaje,id: Long? = null): Long
    suspend fun sincronizar(fecha: String): ResumenSync
    suspend fun anular(id: Long,motivo: String)
    suspend fun omitir(ruta: Long,punto: Long,motivo: String)
    suspend fun cerrar(ruta: Long)
    suspend fun salir()
    fun tienePendientes(): Boolean
}
