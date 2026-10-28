package pe.upeu.ecorecicla.data.local
import kotlinx.coroutines.flow.StateFlow
interface Conectividad { val disponible: StateFlow<Boolean> }
