# SC-B — Auditor de solo lectura

La aplicación usa los privilegios recibidos en el login, también al restaurar la sesión local. AUDITOR recibe PUNTO_LEER, PESAJE_LEER y LECTURA_GLOBAL; no recibe permisos de escritura. Perfil muestra el rol y sus permisos sin un condicional por nombre de rol. Detalle oculta Registrar, Corregir, Anular y Cerrar mediante privilegios existentes.

Cuenta de demostración: auditor1; contraseña configurada en SEED_PASSWORD del backend. Puede descargar la ruta de la fecha y consultar sus pesajes, incluso en caché sin conexión. La API rechaza las escrituras aunque se invoquen fuera de la interfaz.
