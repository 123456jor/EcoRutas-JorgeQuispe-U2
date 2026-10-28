package pe.upeu.ecorecicla
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import pe.upeu.ecorecicla.presentation.navigation.EcoApp
class MainActivity: ComponentActivity() { override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { EcoApp() } } }
