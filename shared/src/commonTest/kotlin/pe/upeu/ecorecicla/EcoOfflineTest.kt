package pe.upeu.ecorecicla
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlin.test.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import pe.upeu.ecorecicla.db.EcoDatabase
import pe.upeu.ecorecicla.data.local.*
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.remote.dto.*
import pe.upeu.ecorecicla.data.repository.*
import pe.upeu.ecorecicla.domain.model.*
expect fun driverPrueba(): SqlDriver
class EcoOfflineTest {
    private lateinit var driver: SqlDriver
    private lateinit var local: BaseLocal
    private lateinit var session: GestorSesion
    private lateinit var api: ApiPrueba
    private lateinit var repo: EcoRepositorioOffline
    private val plataforma=object: Plataforma {
        var id=0
        override fun ahora()="2026-10-28T12:00:00Z"
        override fun nuevoId()="solicitud-${++id}"
        override fun tokenVigente(token: String)=true
    }
    @BeforeTest fun setup() {
        driver=driverPrueba();local=BaseLocal(EcoDatabase(driver))
        val seguro=object: AlmacenSeguro { var valor: String?=null;override fun leer()=valor;override fun guardar(valor: String) {this.valor=valor};override fun borrar() {valor=null} }
        session=GestorSesion(seguro,plataforma).also { it.alLimpiar={local.limpiar()} }
        session.guardar(SesionDto("token-de-prueba","operario1","Jorge","OPERARIO",setOf("PESAJE_CREAR","PESAJE_EDITAR"),"2026-10-28T13:00:00Z"))
        local.q.guardarRuta(1,"2026-10-28","ABIERTA",null)
        local.q.guardarPunto(1,1,"Empresa","Dirección","Norte","Cartón",1,false,null)
        api=ApiPrueba();repo=EcoRepositorioOffline(local,api,session,plataforma)
    }
    @AfterTest fun tearDown()=driver.close()
    private fun form(bruto: Double=30.0)=FormularioPesaje(1,1,"Cartón",bruto,2.0,"Prueba")
    @Test fun conversionDtoConservaAnulacion() {
        val p=PesajeDto(5,1,1,"2026-10-28T12:00:00Z","Cartón",30.0,2.0,28.0,"operario1","ANULADO","Duplicado").dominio()
        assertEquals(EstadoPesaje.Anulado("Duplicado"),p.estado);assertEquals(28.0,p.pesoNeto);assertFalse(p.pendiente)
    }
    @Test fun erroresHttpDistinguenValidacion401403409()=runTest {
        for((status,tipo) in listOf(400 to ErrorApi.Validacion::class,401 to ErrorApi.NoAutorizado::class,403 to ErrorApi.SinPrivilegio::class,409 to ErrorApi.Conflicto::class)) {
            var limpiezas=0
            val client=crearCliente(MockEngine { respond("""{"message":"Rechazado","validationErrors":{"pesoBruto":"Inválido"}}""",HttpStatusCode.fromValue(status),headersOf(HttpHeaders.ContentType,"application/json")) },ApiConfig("http://localhost/"),{"token"})
            try {
                val error=assertFailsWith<ErrorApiException> { EcoApiKtor(client,{limpiezas++}).ruta("2026-10-28") }
                assertEquals(tipo,error.error::class);assertEquals(if(status==401) 1 else 0,limpiezas)
                if(error.error is ErrorApi.Validacion) assertEquals("Inválido",error.error.campos["pesoBruto"])
            } finally {client.close()}
        }
    }
    @Test fun registroSinRedUsaSqlDelightIdNegativoYMarcaPendiente()=runTest {
        api.sinRed=true
        val uno=repo.guardar(form());val dos=repo.guardar(form(40.0))
        assertEquals(-1L,uno);assertEquals(-2L,dos)
        val guardado=local.q.pesaje(uno).executeAsOne()
        assertTrue(guardado.pendiente);assertEquals(30.0,guardado.pesoBruto);assertEquals(0.0,guardado.pesoNeto)
        assertTrue(api.eventos.isEmpty());assertEquals(2,local.q.operaciones().executeAsList().size)
    }
    @Test fun sincronizacionSubeAntesDeDescargarYSustituyeEnTransaccion()=runTest {
        repo.guardar(form());val result=repo.sincronizar("2026-10-28")
        assertEquals(listOf("crear","ruta","pesajes"),api.eventos)
        assertEquals(1,result.enviados);assertTrue(local.q.operaciones().executeAsList().isEmpty())
        assertNull(local.q.pesaje(-1).executeAsOneOrNull());assertFalse(local.q.pesaje(100).executeAsOne().pendiente)
        repo.sincronizar("2026-10-28");assertEquals(1,api.creados.size);assertEquals(1,local.q.pesajes(1).executeAsList().size)
    }
    @Test fun respuestaPerdidaYEdicionPosteriorNoDuplicanNiPierdenDatos()=runTest {
        val id=repo.guardar(form());api.perderRespuesta=true
        val first=repo.sincronizar("2026-10-28");assertEquals(0,first.enviados);assertTrue(local.q.pesaje(id).executeAsOne().pendiente)
        repo.guardar(form(45.0),id)
        assertEquals(2,local.q.operaciones().executeAsList().size)
        api.perderRespuesta=false
        repo.sincronizar("2026-10-28")
        assertEquals(1,api.creados.size);assertTrue(local.q.operaciones().executeAsList().isEmpty())
        val final=local.q.pesaje(100).executeAsOne();assertEquals(45.0,final.pesoBruto);assertEquals(43.0,final.pesoNeto);assertFalse(final.pendiente)
    }
    @Test fun rechazo403ConservaOperacionYNoCierraSesion()=runTest {
        val id=repo.guardar(form());api.rechazar=true
        val result=repo.sincronizar("2026-10-28")
        assertEquals(1,result.errores.size);assertNotNull(session.sesion.value)
        assertTrue(local.q.pesaje(id).executeAsOne().pendiente);assertNotNull(local.q.operaciones().executeAsList().single().error)
    }
    @Test fun consultaObservableSeActualizaYLogoutLimpiaTurno()=runTest {
        assertTrue(repo.observarPesajes(1).first().isEmpty())
        repo.guardar(form());assertEquals(1,repo.observarPesajes(1).first().size)
        repo.salir();assertNull(session.token());assertTrue(local.q.rutas().executeAsList().isEmpty());assertTrue(local.q.pesajes(1).executeAsList().isEmpty());assertTrue(local.q.operaciones().executeAsList().isEmpty())
    }
    @Test fun http204NoIntentaDeserializarYCabeceraEsCentral()=runTest {
        var token: String?=null
        val client=crearCliente(MockEngine { request -> token=request.headers[HttpHeaders.Authorization];respond("",HttpStatusCode.NoContent) },ApiConfig("http://localhost/"),{"abc"})
        try {EcoApiKtor(client,{}).anular(1,"Prueba");assertEquals("Bearer abc",token)} finally {client.close()}
    }
    private class ApiPrueba: EcoApi {
        val eventos=mutableListOf<String>();val creados=mutableMapOf<String,PesajeDto>();var sinRed=false;var perderRespuesta=false;var rechazar=false
        override suspend fun login(usuario: String,clave: String): SesionDto=error("No usado")
        override suspend fun ruta(fecha: String): RutaDto {eventos.add("ruta");return RutaDto(1,fecha,"ABIERTA",listOf(PuntoDto(1,"Empresa","Dirección","Norte","Cartón",1,true)))}
        override suspend fun pesajes(puntoId: Long,pagina: Int): PaginaDto<PesajeDto> {eventos.add("pesajes");return PaginaDto(creados.values.toList(),0,100,creados.size.toLong(),1,true)}
        override suspend fun crear(dto: EscrituraPesajeDto): PesajeDto {
            eventos.add("crear")
            if(sinRed) throw ErrorApiException(ErrorApi.SinConexion())
            if(rechazar) throw ErrorApiException(ErrorApi.SinPrivilegio("No autorizado"))
            val p=creados.getOrPut(dto.solicitudId) {PesajeDto(100L+creados.size,dto.rutaId,dto.puntoId,"2026-10-28T12:00:00Z",dto.material,dto.pesoBruto,dto.tara,dto.pesoBruto-dto.tara,"operario1","REGISTRADO",observacion=dto.observacion)}
            if(perderRespuesta) throw ErrorApiException(ErrorApi.SinConexion())
            return p
        }
        override suspend fun editar(id: Long,dto: EscrituraPesajeDto): PesajeDto {
            eventos.add("editar");val key=creados.entries.first { it.value.id==id }.key
            return creados[key]!!.copy(pesoBruto=dto.pesoBruto,tara=dto.tara,pesoNeto=dto.pesoBruto-dto.tara).also {creados[key]=it}
        }
        override suspend fun anular(id: Long,motivo: String) {}
        override suspend fun omitir(rutaId: Long,puntoId: Long,motivo: String) {}
        override suspend fun cerrar(rutaId: Long) {}
    }
}
