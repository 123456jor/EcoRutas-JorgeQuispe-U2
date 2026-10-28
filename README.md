# EcoRutas — EcoRecicla Android

Autor: **Jorge Quispe**. Aplicación de rutas y pesajes con funcionamiento offline-first. Interfaz en español y paleta verde bosque, conforme a la indicación del autor del proyecto.

## Tecnologías

Kotlin Multiplatform 2.4.10; Android Gradle Plugin 9.0.1; Compose Multiplatform 1.11.1 y Material 3; Ktor 3.6.0; Koin 4.2.2; SQLDelight 2.1.0; Coroutines/StateFlow; Serialization; Android Keystore AES-GCM. Las versiones base se contrastaron con PharmaMobil y se compilaron realmente. El objetivo evaluado es Android; el target JVM permite ejecutar commonTest con SQLite en memoria.

## Arquitectura y estructura

`shared/src/commonMain/kotlin/pe/upeu/ecorecicla`: `domain/model` conserva las propiedades/tipos obligatorios del examen sin anotaciones de red; `domain/repository` declara contratos; `domain/usecase/SincronizarUseCase` coordina el caso de uso; `data/remote` implementa HTTP y DTO separados; `data/local` gestiona SQLDelight y contratos de plataforma; `data/repository` implementa sesión y repositorio offline; `presentation/listado/EcoViewModel` expone StateFlow; `presentation/navigation` contiene las pantallas; `presentation/theme` centraliza colores; `di` declara módulos Koin. Las implementaciones Android están en `androidApp/src/main/kotlin/pe/upeu/ecorecicla`.

## Entorno e instalación

Abre la raíz en Android Studio. Requiere SDK 36, minSdk 24, emulador/dispositivo y JDK compatible con Gradle 9.1. En este equipo se utilizó el JBR 25 de Android Studio. Configura `local.properties` con tu `sdk.dir`; el archivo no se publica.

## Ejecución

Primero levanta EcoAPI según su README. Desde esta raíz:

```powershell
./gradlew.bat :androidApp:assembleDebug
```

Instala `androidApp/build/outputs/apk/debug/androidApp-debug.apk` o pulsa Run en Android Studio. El debug usa `http://10.0.2.2:8080`. HTTP está habilitado únicamente en el manifest de debug. Para release configura `-Peco.apiUrl=https://TU_SERVIDOR` con tu URL real: no hay un servidor público inventado ni una URL de producción preconfigurada. En un teléfono físico usa una URL alcanzable por el dispositivo.

## Pantallas y uso

Login con errores diferenciados; ruta por fecha y búsqueda local; detalle de empresa e historial; alta/corrección de pesajes; anulación y motivo; omisión; cierre de ruta del supervisor; resumen de sincronización; perfil y logout. Las acciones se muestran según privilegios. No existe una lista de rutas o pesajes incrustada en Android.

## API y cliente Ktor

Backend asociado: [EcoAPI-JorgeQuispe-U2](https://github.com/123456jor/EcoAPI-JorgeQuispe-U2). Consulta su README para el contrato completo. EcoApiKtor consume login, rutas, pesajes paginados, altas, correcciones, anulaciones, omisiones y cierre. Koin proporciona una única instancia HttpClient con ContentNegotiation, ignoreUnknownKeys, base URL, timeout y Bearer centralizado. No se registran contraseñas ni tokens. Las páginas se recorren hasta `ultima=true`; 204 solo comprueba el estado.

## Sesión segura

AlmacenSeguroAndroid cifra la sesión completa con AES/GCM y una clave no exportable de Android Keystore. Preferencias contienen IV y ciphertext, no el token en claro. Se desactivan copias de seguridad. Al reabrir, PlataformaAndroid comprueba exp para decidir si recuperar sesión; esta lectura local no sustituye la firma verificada por EcoAPI. Un 401 limpia sesión y base; un 403 conserva la sesión. Logout espera el mutex de operaciones y borra preferencias, rutas, puntos, pesajes y cola. Si hay pendientes pide una decisión explícita: cancelar y sincronizar o descartarlos al salir.

## SQLDelight y fuente de datos

Esquema y consultas: `shared/src/commonMain/sqldelight/pe/upeu/ecorecicla/db/Eco.sq`. Tablas RutaLocal, PuntoLocal, PesajeLocal y Operacion. BaseLocal transforma Query.asFlow() en Flow; el ViewModel observa esos flujos y Compose consume StateFlow. La respuesta HTTP nunca alimenta directamente la lista principal. Buscar por empresa/dirección también funciona offline.

## Registro sin conexión

Guardar siempre escribe primero una fila local y su operación dentro de una transacción. La consulta siguienteId devuelve -1 con tabla vacía y continúa -2, -3… sin colisionar con IDs positivos. La fila muestra pendiente=true. El neto local se presenta como estimación; la solicitud no incluye pesoNeto.

## Orden de sincronización y no duplicación

1. Leer la cola persistente, en orden.
2. Enviar cada operación con solicitudId estable.
3. Confirmar la respuesta e ID del servidor.
4. Sustituir ID provisional y referencias de operaciones en una transacción.
5. Eliminar solo la operación aceptada; conservar ediciones posteriores pendientes.
6. Descargar ruta y todas las páginas de pesajes.
7. Actualizar SQLDelight sin sobrescribir filas pendientes.
8. Flow actualiza la UI.

Un mutex evita sincronizaciones simultáneas. Si se pierde la respuesta de un alta, la solicitud original queda inmutable; una corrección posterior se encola detrás de ella. El servidor devuelve el mismo ID al reintentar y luego se envía la edición. Los rechazos confirmados 400/403/404/409 se conservan con su error; corregirlos repara la operación sin otra alta. Hay botón manual y NetworkCallback para sincronizar al volver la red. Omisión, anulación y cierre requieren conexión; altas/correcciones funcionan offline.

## Errores y estados

ErrorApi distingue Validacion, NoAutorizado, SinPrivilegio, NoEncontrado, Conflicto, Servidor, SinConexion y TiempoAgotado. El ViewModel maneja carga, datos, vacío y mensajes. Los errores 400 del servidor llegan debajo de los campos; 401 limpia el turno, 403 mantiene sesión, 404 informa y refresca, 409 muestra el motivo y 500 permite reintentar. El usuario conserva información local si no hay red.

## Reglas de negocio

RN-01: una ruta cerrada rechaza altas/correcciones con 409. RN-02: el operario corrige solo registros propios del mismo día en America/Lima; el supervisor dispone de edición global mediante privilegios. RN-03: EcoAPI calcula neto = bruto - tara y exige neto > 0, con 400 por campo. RN-04: el cierre exige pesaje no anulado o motivo de omisión en cada punto; 409 indica cuántos faltan. La anulación es lógica y conserva motivo y auditoría.

## Roles y privilegios

| Privilegio | OPERARIO | SUPERVISOR | ADMINISTRADOR |
|---|---|---|---|
| PUNTO_LEER | Sí | Sí | Sí |
| PESAJE_LEER | Sí | Sí | Sí |
| PESAJE_CREAR | Sí | Sí | No |
| PESAJE_EDITAR | Propios del día | Todos | No |
| PESAJE_ANULAR | No | Sí | No |
| RUTA_CERRAR | No | Sí | No |
| PUNTO_GESTIONAR | No | No | Sí |

Las autorizaciones se comprueban en servicios mediante @PreAuthorize y pertenencia a ruta. Los controladores no comparan nombres de rol. Los permisos efectivos se leen de la base y se intersectan con los claims firmados del token.

## Contrato común

Paginación: `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas`, `ultima`. Una ruta es un agregado con sus puntos ordenados, no un listado paginado independiente.

Errores: `timestamp`, `status`, `error`, `message`, `path`, `validationErrors`. 400 indica validación; 401 token ausente/alterado/vencido; 403 privilegio o propiedad insuficiente; 404 recurso inexistente; 409 conflicto de negocio; 500 error interno. 204 nunca contiene JSON.

## Git y trazabilidad

main recibe fusiones desde develop. Cada funcionalidad se implementa en feature/* y se integra mediante PR con merge commit; las correcciones usan fix/*. Autor de todos los commits: Jorge Quispe <joorge0148@gmail.com>. El historial comenzó realmente el 28/10/2026. No acredita jornadas anteriores: no se alteraron fechas ni se fabricaron commits. Los PR remotos conservan las ramas.

La Parte II todavía no está asignada por el docente. No se creó ninguna rama sc-* ni se simularon sus tres commits durante el bloque de 80 minutos. El tag de producto identifica la versión base verificada; si la evaluación incorpora un cambio posterior, se deberá acordar explícitamente la etiqueta correspondiente sin mover un tag publicado silenciosamente.


## Pruebas commonTest

```powershell
./gradlew.bat :shared:jvmTest
```

Se ejecutan las pruebas de `commonTest` usando MockEngine para HTTP y un driver SQLite JDBC en memoria que usa el mismo esquema SQLDelight de producción. Los dobles solo viven en fuentes de test. Reporte: `shared/build/reports/tests/jvmTest/index.html`. Se verifican conversión DTO, errores HTTP, ID negativo, persistencia offline, orden de sincronización, respuesta perdida con edición sin duplicados, recuperación de rechazos, logout y HTTP 204/Bearer central. Hay implementación del driver en jvmTest y androidHostTest.

## Verificación real y ejemplos

En Pixel_8_Pro se probó login correcto/incorrecto, recuperación al reiniciar, ruta real, validación bajo campo, registro con modo avión, ID -1 en SQLite, recuperación de red y sincronización automática con ID definitivo 22. PostgreSQL confirmó una sola fila con observación Offline-Jorge-20261028, incluso tras corregir y sincronizar otra vez. El logout dejó cuatro tablas en cero y preferencias vacías. Las capturas y resultados se conservan en la entrega local de EcoRecicla.

## Decisiones y limitaciones

La UI usa datos locales; se priorizaron seguridad, transacciones y conservación de operaciones frente a animaciones. El target JVM se usa para pruebas y no representa una app de escritorio. La gestión administrativa de padrón/asignaciones se demuestra mediante EcoAPI/Postman; el administrador sin ruta ve el estado vacío y su perfil. No hay demostración iOS ni despliegue público. La Parte II está pendiente de asignación del docente; tampoco se puede acreditar retrospectivamente un historial de varias jornadas.
