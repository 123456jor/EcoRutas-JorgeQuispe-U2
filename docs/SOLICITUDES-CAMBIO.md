# Solicitudes de cambio de la Parte II

El docente solicitó las cuatro solicitudes. Cada rama parte de main de la entrega inicial y conserva su alcance independiente:

| Solicitud | Rama | Implementación |
|---|---|---|
| SC-A | sc-a/solo-pendientes | SQL local combina búsqueda y pendientes; considera altas locales y omisiones justificadas. |
| SC-B | sc-b/auditor-lectura | La app interpreta los privilegios recibidos; auditor1 consulta y no dispone de acciones de escritura. Requiere rama homónima de EcoAPI. |
| SC-C | sc-c/humedad-estimada | Porcentaje 0–100 con hasta 2 decimales; DTO, dominio, cola, SQLDelight, migración 1→2, formulario e historial. Requiere rama homónima de EcoAPI. |
| SC-D | sc-d/distintivo-sincronizacion | COUNT DISTINCT observable en SQLDelight por ruta; no cuenta dos veces una alta seguida de edición. |

`cambios/integracion-sc-a-b-c-d` reúne las cuatro para ejecutar la aplicación. main y la etiqueta de la Parte I conservan la entrega original. No cambiar entre ramas independientes contra una instalación ya migrada; usar la integración para la demostración conjunta.

Validación integrada: 12 pruebas JVM comunes, ninguna falla; APK debug compilado. EcoAPI: 12 pruebas de integración, ninguna falla. Incluyen filtro y alta sin red, humedad en cola y sincronización, auditor sin asignación y rechazo de escrituras, conteo único y vaciado tras sincronizar.

Demostración: sincronizar como operario; activar Solo pendientes y buscar por dirección; desconectar la red del emulador; registrar humedad 12.5 y bruto 25/tara 2; regresar a Mi ruta y ver el distintivo; reconectar y sincronizar. El distintivo vuelve a cero y el historial conserva humedad y neto 23. Ingresar como auditor1 para comprobar las consultas sin botones de escritura.
