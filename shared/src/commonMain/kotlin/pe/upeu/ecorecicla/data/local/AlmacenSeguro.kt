package pe.upeu.ecorecicla.data.local
interface AlmacenSeguro { fun leer(): String?; fun guardar(valor: String); fun borrar() }
interface Plataforma { fun ahora(): String; fun nuevoId(): String; fun tokenVigente(token: String): Boolean }
