package pe.upeu.ecorecicla.domain.model
data class Pesaje(val id: Long,val puntoId: Long,val fechaHora: String,val material: String,val pesoBruto: Double,val tara: Double,val pesoNeto: Double,val operario: String,val estado: EstadoPesaje,val pendiente: Boolean,val rutaId: Long,val observacion: String = "",val errorSincronizacion: String? = null)
