package pe.upeu.ecorecicla
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
class AlmacenSeguroAndroid(context: Context): AlmacenSeguro {
    private val prefs=context.getSharedPreferences("eco_sesion_cifrada",Context.MODE_PRIVATE)
    private val alias="EcoRutasSesionAES"
    private fun clave(): SecretKey {
        val ks=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias,null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override fun guardar(valor: String) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE,clave())
        val bytes=cipher.doFinal(valor.toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("iv",Base64.encodeToString(cipher.iv,Base64.NO_WRAP)).putString("datos",Base64.encodeToString(bytes,Base64.NO_WRAP)).commit())
    }
    override fun leer(): String? {
        val data=prefs.getString("datos",null) ?: return null
        val iv=prefs.getString("iv",null) ?: return null
        val cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE,clave(),GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)))
        return cipher.doFinal(Base64.decode(data,Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }
    override fun borrar() { check(prefs.edit().clear().commit()) }
}
