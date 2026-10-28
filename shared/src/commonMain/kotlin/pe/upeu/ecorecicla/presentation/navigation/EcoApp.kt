package pe.upeu.ecorecicla.presentation.navigation
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.compose.getKoin
import pe.upeu.ecorecicla.presentation.listado.EcoViewModel
import pe.upeu.ecorecicla.presentation.theme.*
import pe.upeu.ecorecicla.domain.model.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EcoApp() {
    val koin=getKoin()
    val vm: EcoViewModel=viewModel { koin.get() }
    val sesion by vm.sesion.collectAsState()
    val page by vm.pantalla.collectAsState()
    val busy by vm.ocupado.collectAsState()
    val message by vm.mensaje.collectAsState()
    val online by vm.conectado.collectAsState()
    EcoTheme {
        if(sesion==null) { Login(vm,busy,message); return@EcoTheme }
        Scaffold(
            topBar={ Column {
                TopAppBar(title={ Column { Text("EcoRutas",fontWeight=FontWeight.Bold); Text("RECICLAR NOS MUEVE",fontSize=10.sp,letterSpacing=2.sp) } },colors=TopAppBarDefaults.topAppBarColors(containerColor=VerdeBosque,titleContentColor=Color.White),navigationIcon={ if(page in listOf("detalle","formulario")) TextButton(onClick={vm.pantalla.value=if(page=="formulario") "detalle" else "ruta"}) { Text("‹",color=Color.White,fontSize=30.sp) } })
                if(!online) Surface(color=VerdeClaro,modifier=Modifier.fillMaxWidth()) { Text("Sin conexión · puedes seguir registrando pesajes",Modifier.padding(horizontal=20.dp,vertical=10.dp),fontSize=12.sp) }
                if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            } },
            bottomBar={ if(page !in listOf("formulario","detalle")) NavigationBar(containerColor=Color.White) {
                listOf(Triple("ruta","⌂","Mi ruta"),Triple("sync","↻","Sincronizar"),Triple("perfil","◉","Perfil")).forEach { (id,icon,label) -> NavigationBarItem(selected=page==id,onClick={vm.pantalla.value=id;vm.mensaje.value=null},icon={Text(icon,fontSize=24.sp)},label={Text(label)}) }
            } }
        ) { padding -> Column(Modifier.padding(padding).fillMaxSize()) {
            message?.let { Surface(color=MaterialTheme.colorScheme.secondaryContainer,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),shape=RoundedCornerShape(12.dp)) { Text(it,Modifier.padding(14.dp),fontSize=13.sp) } }
            when(page) {
                "detalle" -> Detalle(vm,busy)
                "formulario" -> Formulario(vm,busy)
                "perfil" -> Perfil(vm)
                "sync" -> Sincronizacion(vm,busy)
                else -> RutaScreen(vm,busy)
            }
        } }
    }
}
@Composable private fun Login(vm: EcoViewModel,busy: Boolean,message: String?) {
    var usuario by remember { mutableStateOf("") }; var clave by remember { mutableStateOf("") }; var visible by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Fondo).verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().background(VerdeBosque).padding(horizontal=28.dp,vertical=48.dp)) {
            Text("♻",fontSize=58.sp,color=Color(0xFFBFE6A9))
            Spacer(Modifier.height(12.dp)); Text("EcoRutas",color=Color.White,fontSize=38.sp,fontWeight=FontWeight.Bold)
            Text("Cada recojo cuenta.",color=Color(0xFFD2E5D6),fontSize=18.sp)
            Spacer(Modifier.height(22.dp)); Text("RECICLA · REGISTRA · TRANSFORMA",color=Color(0xFFD2E5D6),fontSize=10.sp,letterSpacing=1.5.sp)
        }
        Column(Modifier.padding(28.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("Comienza tu recorrido",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text("Ingresa con tu cuenta de EcoRecicla.",color=Color(0xFF617569))
            OutlinedTextField(usuario,{usuario=it},label={Text("Usuario")},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!busy,shape=RoundedCornerShape(14.dp))
            OutlinedTextField(clave,{clave=it},label={Text("Contraseña")},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!busy,visualTransformation=if(visible) VisualTransformation.None else PasswordVisualTransformation(),trailingIcon={TextButton(onClick={visible=!visible}) {Text(if(visible) "Ocultar" else "Ver")}},shape=RoundedCornerShape(14.dp))
            message?.let { Text(it,color=MaterialTheme.colorScheme.error,fontSize=13.sp) }
            Button(onClick={vm.login(usuario,clave)},enabled=!busy,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(14.dp)) { if(busy) CircularProgressIndicator(Modifier.size(22.dp),color=Color.White,strokeWidth=2.dp) else Text("Ingresar",fontSize=16.sp) }
            Text("Tu ruta también funciona sin conexión después de sincronizar.",fontSize=12.sp,color=Color(0xFF617569))
            Spacer(Modifier.height(12.dp)); Text("EcoRecicla  /  Jorge Quispe",fontSize=11.sp,color=Color(0xFF617569))
        }
    }
}
@Composable private fun Etiqueta(texto: String) { Surface(color=VerdeClaro,shape=RoundedCornerShape(8.dp)) { Text(texto,Modifier.padding(horizontal=10.dp,vertical=5.dp),fontSize=11.sp,color=VerdeBosque) } }
@Composable private fun RutaScreen(vm: EcoViewModel,busy: Boolean) {
    val ruta by vm.ruta.collectAsState(); val puntos by vm.puntos.collectAsState(); val busqueda by vm.busqueda.collectAsState(); val fecha by vm.fecha.collectAsState(); val sesion by vm.sesion.collectAsState()
    var confirmar by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Text("Tu recorrido de hoy",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Text("Recuperamos materiales. Cuidamos el futuro.",fontSize=13.sp,color=Color(0xFF617569)) }
        item { OutlinedTextField(fecha,{vm.fecha.value=it},label={Text("Fecha de ruta · AAAA-MM-DD")},singleLine=true,modifier=Modifier.fillMaxWidth(),trailingIcon={TextButton(onClick=vm::sincronizar,enabled=!busy) {Text("Consultar")}}) }
        item { Card(colors=CardDefaults.cardColors(containerColor=VerdeBosque),shape=RoundedCornerShape(20.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(if(ruta==null) "Sin ruta disponible" else "RUTA #${ruta!!.id}",color=Color.White,fontWeight=FontWeight.Bold,fontSize=18.sp)
            Text(ruta?.let { "${it.fecha}  ·  ${it.estado.lowercase().replaceFirstChar { c -> c.uppercase() }}" } ?: "Sincroniza para descargar tu asignación",color=Color(0xFFD2E5D6),fontSize=13.sp)
            Text("Última sincronización: ${ruta?.ultimaSincronizacion?.replace('T',' ')?.take(19) ?: "Sin sincronizar"}",color=Color(0xFFD2E5D6),fontSize=11.sp)
        } } }
        item { OutlinedTextField(busqueda,{vm.busqueda.value=it},placeholder={Text("Buscar empresa o dirección")},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(14.dp)) }
        if(puntos.isEmpty()) item { Text(if(busy) "Consultando tu ruta…" else "No hay puntos para mostrar. Comprueba la fecha o sincroniza.",Modifier.padding(vertical=20.dp),color=Color(0xFF617569)) }
        items(puntos,key={it.id}) { punto -> Card(onClick={vm.abrir(punto)},colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Surface(color=VerdeClaro,shape=RoundedCornerShape(12.dp)) { Text(punto.secuencia.toString().padStart(2,'0'),Modifier.padding(12.dp),fontWeight=FontWeight.Bold,color=VerdeBosque) }
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) { Text(punto.razonSocial,fontWeight=FontWeight.Bold); Text(punto.direccion,fontSize=12.sp,color=Color(0xFF617569)); Text("${punto.zona} · ${punto.materialHabitual}",fontSize=11.sp,color=Color(0xFF617569)); Etiqueta(if(punto.atendido) "Pesado" else if(punto.motivoOmision!=null) "Omisión justificada" else "Pendiente de pesar") }
                Text("›",color=VerdeBosque,fontSize=24.sp)
            }
        } }
        if(ruta?.estado=="ABIERTA" && sesion?.privilegios?.contains("RUTA_CERRAR")==true) item { OutlinedButton(onClick={confirmar=true},enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text("Cerrar ruta")} }
    }
    if(confirmar) AlertDialog(onDismissRequest={confirmar=false},title={Text("Cerrar la ruta")},text={Text("El servidor verificará que todos los puntos tengan un pesaje válido o una omisión justificada.")},confirmButton={TextButton(onClick={confirmar=false;vm.cerrar()}) {Text("Cerrar ruta")}},dismissButton={TextButton(onClick={confirmar=false}) {Text("Cancelar")}})
}
@Composable private fun Detalle(vm: EcoViewModel,busy: Boolean) {
    val punto by vm.seleccionado.collectAsState(); val pesajes by vm.pesajes.collectAsState(); val ruta by vm.ruta.collectAsState(); val sesion by vm.sesion.collectAsState()
    var anulando by remember { mutableStateOf<Long?>(null) }; var omitiendo by remember { mutableStateOf(false) }; var motivo by remember { mutableStateOf("") }
    val permisos=sesion?.privilegios.orEmpty()
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Text(punto?.razonSocial.orEmpty(),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Text(punto?.direccion.orEmpty(),color=Color(0xFF617569)); Spacer(Modifier.height(10.dp)); Etiqueta("${punto?.zona} · ${punto?.materialHabitual}") }
        if("PESAJE_CREAR" in permisos && ruta?.estado=="ABIERTA") item { Button(onClick={vm.formulario()},enabled=!busy,modifier=Modifier.fillMaxWidth().height(50.dp)) {Text("+ Registrar pesaje")}; TextButton(onClick={omitiendo=true;motivo=""},enabled=!busy) {Text("Registrar motivo de omisión")} }
        item { Text("Historial de pesajes",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold) }
        if(pesajes.isEmpty()) item { Text("Aún no hay pesajes guardados para este punto.",color=Color(0xFF617569)) }
        items(pesajes,key={it.id}) { p -> Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(16.dp)) { Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(p.material,fontWeight=FontWeight.Bold); Etiqueta(if(p.pendiente) "Pendiente de envío" else if(p.estado is EstadoPesaje.Anulado) "Anulado" else "Sincronizado") }
            Text(if(p.pendiente) "Neto estimado: ${p.pesoBruto-p.tara} kg" else "${p.pesoNeto} kg netos",fontWeight=FontWeight.Bold,fontSize=20.sp)
            Text("Bruto ${p.pesoBruto} kg  ·  Tara ${p.tara} kg",fontSize=12.sp)
            Text("ID ${p.id} · ${p.fechaHora.replace('T',' ').take(19)}",fontSize=11.sp,color=Color(0xFF617569)); Text("Registrado por ${p.operario}",fontSize=11.sp,color=Color(0xFF617569))
            if(p.observacion.isNotBlank()) Text(p.observacion,fontSize=12.sp)
            (p.estado as? EstadoPesaje.Anulado)?.let { Text("Motivo: ${it.motivo}",fontSize=12.sp,color=MaterialTheme.colorScheme.error) }
            p.errorSincronizacion?.let { Text(it,color=MaterialTheme.colorScheme.error,fontSize=12.sp) }
            if(p.estado is EstadoPesaje.Registrado) Row {
                if("PESAJE_EDITAR" in permisos && ruta?.estado=="ABIERTA" && (p.operario==sesion?.usuario || "PESAJE_ANULAR" in permisos)) TextButton(onClick={vm.formulario(p)},enabled=!busy) {Text("Corregir")}
                if("PESAJE_ANULAR" in permisos && !p.pendiente && ruta?.estado=="ABIERTA") TextButton(onClick={anulando=p.id;motivo=""},enabled=!busy) {Text("Anular")}
            }
        } } }
    }
    if(anulando!=null || omitiendo) AlertDialog(onDismissRequest={anulando=null;omitiendo=false},title={Text(if(omitiendo) "Justificar omisión" else "Anular pesaje")},text={OutlinedTextField(motivo,{motivo=it},label={Text("Motivo obligatorio")},modifier=Modifier.fillMaxWidth())},confirmButton={TextButton(enabled=motivo.isNotBlank(),onClick={if(omitiendo) vm.omitir(motivo) else vm.anular(anulando!!,motivo);anulando=null;omitiendo=false}) {Text("Confirmar")}},dismissButton={TextButton(onClick={anulando=null;omitiendo=false}) {Text("Cancelar")}})
}
@Composable private fun Formulario(vm: EcoViewModel,busy: Boolean) {
    val f by vm.formulario.collectAsState(); val online by vm.conectado.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Datos del pesaje",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("El peso neto definitivo lo calcula EcoAPI.",color=Color(0xFF617569),fontSize=13.sp)
        if(!online) Etiqueta("Se guardará pendiente en este dispositivo")
        listOf(Triple("material","Material",f.material),Triple("pesoBruto","Peso bruto (kg)",f.bruto),Triple("tara","Tara (kg)",f.tara),Triple("observacion","Observación",f.observacion)).forEach { (campo,label,valor) ->
            OutlinedTextField(valor,{vm.cambiar(campo,it)},label={Text(label)},isError=campo in f.errores,supportingText={f.errores[campo]?.let {Text(it)}},keyboardOptions=KeyboardOptions(keyboardType=if(campo in listOf("pesoBruto","tara")) KeyboardType.Decimal else KeyboardType.Text),modifier=Modifier.fillMaxWidth(),enabled=!busy,singleLine=campo!="observacion",shape=RoundedCornerShape(14.dp))
        }
        val bruto=f.bruto.replace(',','.').toDoubleOrNull(); val tara=f.tara.replace(',','.').toDoubleOrNull()
        if(bruto!=null && tara!=null) Text("Referencia local: ${bruto-tara} kg (estimación)",fontSize=13.sp,color=VerdeBosque)
        Button(onClick=vm::guardar,enabled=!busy,modifier=Modifier.fillMaxWidth().height(54.dp)) {Text("Guardar pesaje")}
    }
}
@Composable private fun Sincronizacion(vm: EcoViewModel,busy: Boolean) {
    val result by vm.resumen.collectAsState(); val ruta by vm.ruta.collectAsState(); val online by vm.conectado.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text("Todo en su lugar",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Primero enviamos tus registros pendientes. Después actualizamos tu ruta.",color=Color(0xFF617569))
        Etiqueta(if(online) "Con conexión" else "Sin conexión")
        Card(colors=CardDefaults.cardColors(containerColor=Color.White)) { Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Último resultado",fontWeight=FontWeight.Bold)
            Text("Enviados: ${result?.enviados ?: 0}")
            Text("Recibidos: ${result?.recibidos ?: 0}")
            Text("Última sincronización: ${ruta?.ultimaSincronizacion ?: "Sin sincronizar"}",fontSize=12.sp)
            Text(if(vm.hayPendientes()) "Hay operaciones pendientes de envío" else "No hay operaciones pendientes",fontSize=13.sp)
            result?.errores?.forEach {Text(it,fontSize=12.sp,color=MaterialTheme.colorScheme.error)}
        } }
        Button(onClick=vm::sincronizar,enabled=!busy,modifier=Modifier.fillMaxWidth().height(52.dp)) {Text(if(busy) "Sincronizando…" else "Sincronizar ahora")}
        Text("Si una operación es rechazada, sus datos permanecen en el dispositivo para revisarlos.",fontSize=12.sp,color=Color(0xFF617569))
    }
}
@Composable private fun Perfil(vm: EcoViewModel) {
    val s by vm.sesion.collectAsState(); var confirmar by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Mi perfil",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text(s?.nombre.orEmpty(),style=MaterialTheme.typography.titleLarge); Text(s?.usuario.orEmpty(),color=Color(0xFF617569)); Etiqueta(s?.rol.orEmpty())
        HorizontalDivider(); Text("Privilegios de tu cuenta",fontWeight=FontWeight.Bold)
        s?.privilegios?.sorted()?.forEach { Text("✓  ${it.replace('_',' ').lowercase().replaceFirstChar { c -> c.uppercase() }}",fontSize=14.sp) }
        Spacer(Modifier.height(10.dp)); OutlinedButton(onClick={confirmar=true},modifier=Modifier.fillMaxWidth()) {Text("Cerrar sesión")}
        Text("Equipo compartido: al salir se elimina la sesión y se limpian los datos del turno.",fontSize=12.sp,color=Color(0xFF617569))
    }
    if(confirmar) AlertDialog(onDismissRequest={confirmar=false},title={Text("Cerrar sesión")},text={Text(if(vm.hayPendientes()) "Hay registros sin enviar. Si sales ahora se descartarán al limpiar el equipo. Puedes cancelar y sincronizar primero." else "Se borrará la sesión y la información local de este turno.")},confirmButton={TextButton(onClick={confirmar=false;vm.salir()}) {Text(if(vm.hayPendientes()) "Descartar y salir" else "Salir")}},dismissButton={TextButton(onClick={confirmar=false}) {Text("Cancelar")}})
}
