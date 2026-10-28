package pe.upeu.ecorecicla.domain.usecase
import pe.upeu.ecorecicla.domain.repository.EcoRepositorio
class SincronizarUseCase(private val repo: EcoRepositorio) { suspend operator fun invoke(fecha: String)=repo.sincronizar(fecha) }
