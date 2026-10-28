package pe.upeu.ecorecicla
import android.app.Application
import org.koin.core.context.startKoin
import org.koin.android.ext.koin.androidContext
import pe.upeu.ecorecicla.di.*
class EcoApplication: Application() { override fun onCreate() { super.onCreate(); startKoin { androidContext(this@EcoApplication); modules(platformModule,dataModule,redModule,domainModule,presentationModule) } } }
