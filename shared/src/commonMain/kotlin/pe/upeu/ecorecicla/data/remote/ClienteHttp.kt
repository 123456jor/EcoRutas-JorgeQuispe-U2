package pe.upeu.ecorecicla.data.remote
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
fun crearCliente(engine: HttpClientEngine,config: ApiConfig,token: () -> String?): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
    install(HttpTimeout) { requestTimeoutMillis=20000; connectTimeoutMillis=10000; socketTimeoutMillis=20000 }
    expectSuccess=false
    defaultRequest {
        url(config.baseUrl.trimEnd('/')+"/")
        contentType(ContentType.Application.Json)
        token()?.let { bearerAuth(it) }
    }
    // No se registra el cuerpo, contraseñas ni cabeceras de autorización.
}
