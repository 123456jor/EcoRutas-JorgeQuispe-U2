package pe.upeu.ecorecicla.presentation.listado
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import pe.upeu.ecorecicla.data.local.*
import pe.upeu.ecorecicla.data.remote.EcoApi
import pe.upeu.ecorecicla.data.repository.GestorSesion
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
data class FormularioUi(val material: String="",val bruto: String="",val tara: String="",val observacion: String="",val errores: Map<String,String> = emptyMap())
@OptIn(ExperimentalCoroutinesApi::class)
class EcoViewModel(private val repo: EcoRepositorio,private val api: EcoApi,private val gestor: GestorSesion,private val sync: SincronizarUseCase,conectividad: Conectividad): ViewModel() {
    val sesion=gestor.sesion
    val conectado=conectividad.disponible
    val pantalla=MutableStateFlow("ruta")
    val ocupado=MutableStateFlow(false)
    val mensaje=MutableStateFlow<String?>(null)
    val busqueda=MutableStateFlow("")
    val fecha=MutableStateFlow(Clock.System.now().toLocalDateTime(TimeZone.of("America/Lima")).date.toString())
    val ruta=repo.observarRuta().stateIn(viewModelScope,SharingStarted.Eagerly,null)
    val puntos=combine(ruta,busqueda) { r,b -> r?.id to b }.flatMapLatest { (id,b) -> if(id==null) flowOf(emptyList()) else repo.observarPuntos(id,b) }.stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val seleccionado=MutableStateFlow<PuntoAcopio?>(null)
    val pesajes=seleccionado.flatMapLatest { p -> if(p==null) flowOf(emptyList()) else repo.observarPesajes(p.id) }.stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val formulario=MutableStateFlow(FormularioUi())
    val resumen=MutableStateFlow<ResumenSync?>(null)
    private var editandoId: Long?=null
    private var editandoRutaId: Long?=null
    init {
        gestor.restaurar()
        viewModelScope.launch {
            combine(sesion,conectado) { s,c -> s?.usuario to c }.distinctUntilChanged().collect { (u,c) -> if(u!=null && c) sincronizar() }
        }
    }
    private fun ejecutar(block: suspend () -> Unit) {
        if(ocupado.value) return
        viewModelScope.launch {
            ocupado.value=true; mensaje.value=null
            try { withContext(Dispatchers.Default) { block() } }
            catch(e: CancellationException) { throw e }
            catch(e: ErrorApiException) {
                mensaje.value=e.error.mensaje
                if(e.error is ErrorApi.Validacion) formulario.value=formulario.value.copy(errores=e.error.campos)
                if(e.error is ErrorApi.NoAutorizado) { gestor.limpiar(); pantalla.value="ruta"; seleccionado.value=null }
                if(e.error is ErrorApi.NoEncontrado && sesion.value!=null) runCatching { withContext(Dispatchers.Default) { sync(fecha.value) } }
            }
            catch(e: Exception) { mensaje.value="No se pudo completar la operación. Intenta nuevamente." }
            finally { ocupado.value=false }
        }
    }
    fun login(usuario: String,clave: String) {
        if(usuario.isBlank() || clave.isBlank()) { mensaje.value="Completa usuario y contraseña"; return }
        ejecutar { gestor.guardar(api.login(usuario,clave)); pantalla.value="ruta"; try { resumen.value=sync(fecha.value) } catch(e: ErrorApiException) { mensaje.value=e.error.mensaje } }
    }
    fun sincronizar()=ejecutar { resumen.value=sync(fecha.value); mensaje.value=resumen.value!!.errores.firstOrNull() ?: "Sincronización completada" }
    fun abrir(punto: PuntoAcopio) { seleccionado.value=punto; pantalla.value="detalle"; mensaje.value=null }
    fun formulario(pesaje: Pesaje?=null) {
        editandoId=pesaje?.id; editandoRutaId=pesaje?.rutaId
        formulario.value=pesaje?.let { FormularioUi(it.material,it.pesoBruto.toString(),it.tara.toString(),it.observacion) } ?: FormularioUi(material=seleccionado.value?.materialHabitual.orEmpty())
        mensaje.value=null; pantalla.value="formulario"
    }
    fun cambiar(campo: String,valor: String) { formulario.value=formulario.value.let {
        when(campo) { "material" -> it.copy(material=valor); "pesoBruto" -> it.copy(bruto=valor); "tara" -> it.copy(tara=valor); else -> it.copy(observacion=valor) }.copy(errores=it.errores-campo)
    } }
    fun guardar()=ejecutar {
        val f=formulario.value; val r=ruta.value ?: return@ejecutar; val punto=seleccionado.value ?: return@ejecutar
        val data=FormularioPesaje(editandoRutaId ?: r.id,punto.id,f.material,f.bruto.replace(',','.').toDoubleOrNull() ?: Double.NaN,f.tara.replace(',','.').toDoubleOrNull() ?: Double.NaN,f.observacion)
        editandoId=repo.guardar(data,editandoId)
        var pendiente=true
        if(conectado.value) {
            try {
                val result=sync(fecha.value); resumen.value=result
                editandoId=result.ids[editandoId] ?: editandoId
                if(result.campos.isNotEmpty()) { formulario.value=f.copy(errores=result.campos); mensaje.value=result.errores.firstOrNull(); return@ejecutar }
                pendiente=result.errores.isNotEmpty()
            } catch(e: ErrorApiException) {
                if(e.error is ErrorApi.NoAutorizado) throw e
                mensaje.value=e.error.mensaje
            }
        }
        pantalla.value="detalle"; mensaje.value=if(!pendiente) "Pesaje guardado y sincronizado" else "Pesaje guardado en el dispositivo. Pendiente de envío."
    }
    fun anular(id: Long,motivo: String)=ejecutar { repo.anular(id,motivo); sync(fecha.value); mensaje.value="Pesaje anulado" }
    fun omitir(motivo: String)=ejecutar { repo.omitir(ruta.value!!.id,seleccionado.value!!.id,motivo); sync(fecha.value); mensaje.value="Motivo de omisión registrado" }
    fun cerrar()=ejecutar { repo.cerrar(ruta.value!!.id); mensaje.value="Ruta cerrada" }
    fun salir()=ejecutar { repo.salir(); seleccionado.value=null; formulario.value=FormularioUi(); resumen.value=null; pantalla.value="ruta"; busqueda.value="" }
    fun hayPendientes()=repo.tienePendientes()
}
