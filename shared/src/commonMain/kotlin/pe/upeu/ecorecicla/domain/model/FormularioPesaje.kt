package pe.upeu.ecorecicla.domain.model
data class FormularioPesaje(val rutaId: Long,val puntoId: Long,val material: String,val pesoBruto: Double,val tara: Double,val observacion: String = "",val humedadEstimada: Double = 0.0) {
    fun validar(): Map<String,String> = buildMap {
        if(material.isBlank() || material.length>100) put("material","Escribe un material de hasta 100 caracteres")
        if(!pesoBruto.isFinite() || pesoBruto<=0) put("pesoBruto","El peso bruto debe ser positivo")
        if(!tara.isFinite() || tara<0) put("tara","La tara no puede ser negativa")
        if(pesoBruto.isFinite() && tara.isFinite() && pesoBruto<=tara) put("pesoBruto","El peso neto debe ser mayor que cero")
        if(!humedadEstimada.isFinite() || humedadEstimada !in 0.0..100.0 || kotlin.math.abs(humedadEstimada*100-kotlin.math.round(humedadEstimada*100))>0.000001) put("humedadEstimada","Ingrese un porcentaje entre 0 y 100 con hasta 2 decimales")
        if(observacion.length>500) put("observacion","Máximo 500 caracteres")
    }
}
