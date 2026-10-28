package pe.upeu.ecorecicla.presentation.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
val VerdeBosque=Color(0xFF145A3A)
val VerdeClaro=Color(0xFFE3F1DD)
val Tinta=Color(0xFF17342A)
val Fondo=Color(0xFFF4F8F2)
@Composable fun EcoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=lightColorScheme(primary=VerdeBosque,onPrimary=Color.White,primaryContainer=VerdeClaro,onPrimaryContainer=Tinta,secondary=Color(0xFF53774C),background=Fondo,surface=Color.White,onSurface=Tinta,onBackground=Tinta,error=Color(0xFFB3261E)),content=content)
}
