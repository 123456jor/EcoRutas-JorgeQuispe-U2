package pe.upeu.ecorecicla.domain.model
data class Ruta(val id: Long,val fecha: String,val estado: String,val puntos: List<PuntoAcopio>,val ultimaSincronizacion: String?)
