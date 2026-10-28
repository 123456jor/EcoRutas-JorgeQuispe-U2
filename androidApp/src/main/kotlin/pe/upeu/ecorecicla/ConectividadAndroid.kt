package pe.upeu.ecorecicla
import android.content.Context
import android.net.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.upeu.ecorecicla.data.local.Conectividad
class ConectividadAndroid(context: Context): Conectividad {
    private val manager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val estado=MutableStateFlow(actual())
    override val disponible=estado.asStateFlow()
    private fun actual()=manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true
    init { manager.registerDefaultNetworkCallback(object: ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { estado.value=actual() }
        override fun onCapabilitiesChanged(network: Network,capabilities: NetworkCapabilities) { estado.value=actual() }
        override fun onLost(network: Network) { estado.value=actual() }
    }) }
}
