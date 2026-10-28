package pe.upeu.ecorecicla
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import pe.upeu.ecorecicla.data.local.*
import pe.upeu.ecorecicla.data.remote.ApiConfig
val platformModule=module {
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single<Plataforma> { PlataformaAndroid() }
    single<Conectividad> { ConectividadAndroid(androidContext()) }
    single { crearBaseLocal(androidContext()) }
    single<HttpClientEngine> { CIO.create() }
    single { ApiConfig(BuildConfig.API_BASE_URL) }
}
