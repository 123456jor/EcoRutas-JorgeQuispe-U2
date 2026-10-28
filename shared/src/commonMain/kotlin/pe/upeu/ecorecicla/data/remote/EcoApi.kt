package pe.upeu.ecorecicla.data.remote
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import pe.upeu.ecorecicla.data.remote.dto.*
import pe.upeu.ecorecicla.domain.model.*
interface EcoApi {
    suspend fun login(usuario: String,clave: String): SesionDto
    suspend fun ruta(fecha: String): RutaDto
    suspend fun pesajes(puntoId: Long,pagina: Int): PaginaDto<PesajeDto>
    suspend fun crear(dto: EscrituraPesajeDto): PesajeDto
    suspend fun editar(id: Long,dto: EscrituraPesajeDto): PesajeDto
    suspend fun anular(id: Long,motivo: String)
    suspend fun omitir(rutaId: Long,puntoId: Long,motivo: String)
    suspend fun cerrar(rutaId: Long)
}
class EcoApiKtor(private val client: HttpClient,private val expirada: () -> Unit): EcoApi {
    private suspend fun respuesta(block: suspend () -> HttpResponse): HttpResponse {
        try {
            val r=block()
            if(r.status.value !in 200..299) {
                val dto=try { r.body<ErrorDto>() } catch(e: CancellationException) { throw e } catch(e: Exception) { ErrorDto(message="Respuesta inválida del servicio") }
                if(r.status.value==401) expirada()
                throw ErrorApiException(mapearError(r.status.value,dto))
            }
            return r
        } catch(e: CancellationException) { throw e
        } catch(e: ErrorApiException) { throw e
        } catch(e: HttpRequestTimeoutException) { throw ErrorApiException(ErrorApi.TiempoAgotado())
        } catch(e: Exception) {
            if(e.message.orEmpty().contains("timeout",true) || e.message.orEmpty().contains("timed out",true)) throw ErrorApiException(ErrorApi.TiempoAgotado())
            throw ErrorApiException(ErrorApi.SinConexion())
        }
    }
    override suspend fun login(usuario: String,clave: String): SesionDto = respuesta { client.post("api/v1/auth/login") { setBody(LoginDto(usuario,clave)) } }.body()
    override suspend fun ruta(fecha: String): RutaDto = respuesta { client.get("api/v1/rutas/hoy") { parameter("fecha",fecha) } }.body()
    override suspend fun pesajes(puntoId: Long,pagina: Int): PaginaDto<PesajeDto> = respuesta { client.get("api/v1/puntos/$puntoId/pesajes") { parameter("pagina",pagina); parameter("tamanio",100) } }.body()
    override suspend fun crear(dto: EscrituraPesajeDto): PesajeDto = respuesta { client.post("api/v1/pesajes") { setBody(dto) } }.body()
    override suspend fun editar(id: Long,dto: EscrituraPesajeDto): PesajeDto = respuesta { client.put("api/v1/pesajes/$id") { setBody(dto) } }.body()
    override suspend fun anular(id: Long,motivo: String) { respuesta { client.delete("api/v1/pesajes/$id") { parameter("motivo",motivo) } } }
    override suspend fun omitir(rutaId: Long,puntoId: Long,motivo: String) { respuesta { client.put("api/v1/rutas/$rutaId/puntos/$puntoId/omision") { setBody(MotivoDto(motivo)) } } }
    override suspend fun cerrar(rutaId: Long) { respuesta { client.post("api/v1/rutas/$rutaId/cerrar") } }
}
