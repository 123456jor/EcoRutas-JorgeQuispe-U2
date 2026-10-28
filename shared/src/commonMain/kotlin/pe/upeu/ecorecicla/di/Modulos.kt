package pe.upeu.ecorecicla.di
import org.koin.dsl.module
import io.ktor.client.HttpClient
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.local.*
import pe.upeu.ecorecicla.data.repository.*
import pe.upeu.ecorecicla.domain.repository.EcoRepositorio
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
import pe.upeu.ecorecicla.presentation.listado.EcoViewModel
val dataModule=module {
    single { GestorSesion(get(),get()).also { s -> s.alLimpiar={get<BaseLocal>().limpiar()} } }
    single<EcoApi> { EcoApiKtor(get(),{get<GestorSesion>().limpiar()}) }
    single<EcoRepositorio> { EcoRepositorioOffline(get(),get(),get(),get()) }
}
val redModule=module { single<HttpClient> { crearCliente(get(),get(),{get<GestorSesion>().token()}) } }
val domainModule=module { factory { SincronizarUseCase(get()) } }
val presentationModule=module { factory { EcoViewModel(get(),get(),get(),get(),get()) } }
