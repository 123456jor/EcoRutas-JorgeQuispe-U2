package pe.upeu.ecorecicla
import android.util.Base64
import org.json.JSONObject
import java.util.UUID
import java.time.Instant
import pe.upeu.ecorecicla.data.local.Plataforma
class PlataformaAndroid: Plataforma {
    override fun ahora()=Instant.now().toString()
    override fun nuevoId()=UUID.randomUUID().toString()
    override fun tokenVigente(token: String): Boolean = runCatching {
        val payload=JSONObject(String(Base64.decode(token.split('.')[1],Base64.URL_SAFE or Base64.NO_WRAP)))
        payload.getLong("exp")>Instant.now().epochSecond
    }.getOrDefault(false)
    // Solo decide si recuperar la sesión local. El servidor siempre verifica firma y expiración.
}
