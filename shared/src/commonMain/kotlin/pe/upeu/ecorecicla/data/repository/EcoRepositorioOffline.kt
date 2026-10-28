package pe.upeu.ecorecicla.data.repository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import pe.upeu.ecorecicla.data.local.*
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.remote.dto.*
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
class EcoRepositorioOffline(private val local: BaseLocal,private val api: EcoApi,private val sesion: GestorSesion,private val plataforma: Plataforma): EcoRepositorio {
    private val mutex=Mutex()
    private val q get()=local.q
    override fun observarRuta(): Flow<Ruta?> = local.rutas().map { rows -> rows.firstOrNull()?.let { Ruta(it.id,it.fecha,it.estado,emptyList(),it.ultimaSync) } }
    override fun observarPuntos(ruta: Long,busqueda: String)=local.puntos(ruta,busqueda)
    override fun observarPesajes(punto: Long)=local.pesajes(punto)
    override fun tienePendientes()=q.operaciones().executeAsList().isNotEmpty()
    private fun vigente() {
        val token=sesion.token()
        if(token==null || !plataforma.tokenVigente(token)) { sesion.limpiar(); throw ErrorApiException(ErrorApi.NoAutorizado("La sesión venció. Inicia sesión nuevamente.")) }
    }
    override suspend fun guardar(p: FormularioPesaje,id: Long?): Long = mutex.withLock {
        vigente()
        val errores=p.validar(); if(errores.isNotEmpty()) throw ErrorApiException(ErrorApi.Validacion(errores,"Revisa los campos"))
        val permiso=if(id==null) "PESAJE_CREAR" else "PESAJE_EDITAR"
        if(permiso !in sesion.sesion.value!!.privilegios) throw ErrorApiException(ErrorApi.SinPrivilegio("No posee el privilegio requerido"))
        if(q.rutas().executeAsList().firstOrNull { it.id==p.rutaId }?.estado!="ABIERTA") throw ErrorApiException(ErrorApi.Conflicto("La ruta está cerrada"))
        var real=0L
        local.database.transaction {
            real=id ?: q.siguienteId { it ?: -1L }.executeAsOne()
            val old=id?.let { q.pesaje(it).executeAsOneOrNull() }
            if(id!=null && old==null) throw ErrorApiException(ErrorApi.NoEncontrado("Pesaje no encontrado"))
            val solicitud=plataforma.nuevoId()
            q.guardarPesaje(real,p.rutaId,p.puntoId,old?.fechaHora ?: plataforma.ahora(),p.material,p.pesoBruto,p.tara,old?.pesoNeto ?: 0.0,old?.operario ?: sesion.sesion.value!!.usuario,old?.estado ?: "REGISTRADO",old?.motivo,true,p.observacion,null)
            // La operación original queda inmutable, incluso si se perdió su respuesta.
            // Una edición posterior se encola y sigue al alta usando la referencia provisional.
            val rechazada=q.operaciones().executeAsList().firstOrNull { it.pesajeId==real && it.error?.startsWith("RECHAZADO:")==true }
            if(rechazada!=null) q.repararOperacion(Json.encodeToString(p.dto(rechazada.solicitudId)),rechazada.solicitudId)
            else q.encolar(solicitud,real,if(id==null) "CREAR" else "EDITAR",Json.encodeToString(p.dto(solicitud)))
        }
        real
    }
    override suspend fun sincronizar(fecha: String): ResumenSync = mutex.withLock {
        vigente()
        var enviados=0; var recibidos=0
        val errores=mutableListOf<String>()
        val campos=mutableMapOf<String,String>()
        val ids=mutableMapOf<Long,Long>()
        val bloqueados=mutableSetOf<Long>()
        val solicitudes=q.operaciones().executeAsList().map { it.solicitudId }
        for(solicitud in solicitudes) {
            val op=q.operaciones().executeAsList().firstOrNull { it.solicitudId==solicitud } ?: continue
            if(op.pesajeId in bloqueados) continue
            try {
                val dto=Json.decodeFromString<EscrituraPesajeDto>(op.payload)
                val result=if(op.tipo=="CREAR") api.crear(dto) else api.editar(op.pesajeId,dto)
                local.database.transaction {
                    q.borrarOperacion(solicitud)
                    if(op.pesajeId!=result.id) {
                        q.reemplazarReferencias(result.id,op.pesajeId)
                        q.reemplazarId(result.id,op.pesajeId)
                    }
                    if(q.operacionesPesaje(result.id).executeAsOne()==0L) local.guardar(result)
                }
                ids[op.pesajeId]=result.id
                enviados++
            } catch(e: ErrorApiException) {
                if(e.error is ErrorApi.NoAutorizado) throw e
                val definitivo=e.error is ErrorApi.Validacion || e.error is ErrorApi.SinPrivilegio || e.error is ErrorApi.Conflicto || e.error is ErrorApi.NoEncontrado
                val detalle=if(definitivo) "RECHAZADO: ${e.message}" else e.message
                local.database.transaction { q.errorOperacion(detalle,solicitud); q.errorPesaje(e.message,op.pesajeId) }
                (e.error as? ErrorApi.Validacion)?.let { campos.putAll(it.campos) }
                errores.add(e.error.mensaje); bloqueados.add(op.pesajeId)
                if(e.error is ErrorApi.SinConexion || e.error is ErrorApi.TiempoAgotado) return@withLock ResumenSync(enviados,recibidos,errores,campos,ids)
            }
        }
        // Solo después de procesar la cola se descarga y se actualiza la fuente local.
        val ruta=api.ruta(fecha)
        val remotos=mutableListOf<PesajeDto>()
        for(punto in ruta.puntos) {
            var pagina=0
            do {
                val lote=api.pesajes(punto.id,pagina++)
                remotos.addAll(lote.contenido)
            } while(!lote.ultima)
        }
        local.database.transaction {
            q.guardarRuta(ruta.id,ruta.fecha,ruta.estado,plataforma.ahora())
            q.borrarPuntosRuta(ruta.id)
            ruta.puntos.forEach { q.guardarPunto(ruta.id,it.id,it.razonSocial,it.direccion,it.zona,it.materialHabitual,it.secuencia.toLong(),it.atendido,it.motivoOmision) }
            remotos.forEach { dto -> if(q.pesaje(dto.id).executeAsOneOrNull()?.pendiente!=true) local.guardar(dto) }
        }
        recibidos=ruta.puntos.size+remotos.size
        ResumenSync(enviados,recibidos,errores,campos,ids)
    }
    override suspend fun anular(id: Long,motivo: String)=mutex.withLock {
        vigente()
        val old=q.pesaje(id).executeAsOneOrNull() ?: throw ErrorApiException(ErrorApi.NoEncontrado("Pesaje no encontrado"))
        if(old.pendiente) throw ErrorApiException(ErrorApi.Conflicto("Sincroniza el pesaje antes de anularlo"))
        api.anular(id,motivo)
        q.guardarPesaje(old.id,old.rutaId,old.puntoId,old.fechaHora,old.material,old.pesoBruto,old.tara,old.pesoNeto,old.operario,"ANULADO",motivo,false,old.observacion,null)
        Unit
    }
    override suspend fun omitir(ruta: Long,punto: Long,motivo: String) { mutex.withLock { vigente(); api.omitir(ruta,punto,motivo) } }
    override suspend fun cerrar(ruta: Long) { mutex.withLock {
        vigente()
        if(tienePendientes()) throw ErrorApiException(ErrorApi.Conflicto("Sincroniza los pesajes pendientes antes de cerrar"))
        api.cerrar(ruta)
        q.rutas().executeAsList().firstOrNull { it.id==ruta }?.let { q.guardarRuta(it.id,it.fecha,"CERRADA",it.ultimaSync) }
    } }
    override suspend fun salir() { mutex.withLock { sesion.limpiar() } }
}
