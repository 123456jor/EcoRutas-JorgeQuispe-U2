package pe.upeu.ecorecicla.data.remote
data class ApiConfig(val baseUrl: String) { init { require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) } }
