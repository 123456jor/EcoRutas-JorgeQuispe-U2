package pe.upeu.ecorecicla.data.remote
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.data.remote.dto.ErrorDto
fun mapearError(status: Int,body: ErrorDto): ErrorApi = when(status) {
    400 -> ErrorApi.Validacion(body.validationErrors,body.message)
    401 -> ErrorApi.NoAutorizado(body.message)
    403 -> ErrorApi.SinPrivilegio(body.message)
    404 -> ErrorApi.NoEncontrado(body.message)
    409 -> ErrorApi.Conflicto(body.message)
    else -> ErrorApi.Servidor(body.message)
}
