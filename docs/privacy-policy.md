# Política de Privacidad

**Última actualización:** 19 de noviembre de 2026

## 1. Identificación

Esta Política de Privacidad describe el tratamiento de datos de la aplicación **Comentario Bíblico** (en adelante, "la Aplicación"), desarrollada por **johngom99** (en adelante, "el Desarrollador"), disponible en Google Play Store bajo el identificador de paquete `com.comentariobiblico.app`.

## 2. Resumen

La Aplicación es un lector de comentario bíblico que funciona **100 % sin conexión a internet**. No recopila, no transmite, no almacena en servidores remotos ni comparte ningún dato personal del usuario. No solicita permisos del dispositivo, no incluye análisis estadísticos, no muestra publicidad y no utiliza ningún servicio de terceros que pueda acceder a información del usuario.

## 3. Datos que NO recopila la Aplicación

La Aplicación **no recopila** ninguno de los siguientes datos:

- Información personal (nombre, correo electrónico, teléfono, etc.)
- Ubicación geográfica
- Identificadores del dispositivo (IMEI, Android ID, ID de publicidad, etc.)
- Datos de contacto, mensajes, registros de llamadas
- Fotografías, audio o vídeo
- Datos financieros o de pago
- Datos de salud o información sensible de cualquier tipo

## 4. Permisos del dispositivo

La Aplicación **no solicita ningún permiso** del sistema operativo Android. En particular:

- No requiere acceso a internet (`INTERNET`)
- No requiere acceso a ubicación (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`)
- No requiere acceso a cámara, micrófono o almacenamiento externo
- No requiere acceso a contactos, SMS, teléfono o calendario

## 5. Datos almacenados localmente

La Aplicación almacena exclusivamente en el dispositivo del usuario, dentro de su propio espacio privado de almacenamiento interno (`filesDir`), la siguiente información no personal:

| Dato | Descripción |
|---|---|
| Tema de lectura | Preferencia de esquema de color (sistema, claro, oscuro, sepia) |
| Tamaño de fuente | Multiplicador de escala tipográfica |
| Interlineado | Preferencia de espaciado entre líneas |
| Tipografía con serifa | Preferencia booleana |
| Pantalla encendida | Preferencia booleana para mantener la pantalla activa |
| Última posición de lectura | Referencia del último versículo consultado (identificador numérico) |

Estos datos se guardan mediante **AndroidX DataStore Preferences**, un mecanismo estándar de almacenamiento local de Android. Se utilizan exclusivamente para restaurar las preferencias del usuario al volver a abrir la Aplicación.

Adicionalmente, la Aplicación incluye una base de datos SQLite de solo lectura (~43 MB) con el contenido del comentario bíblico (textos bíblicos, comentarios e índices de búsqueda). Este archivo es contenido estático empaquetado con la Aplicación y no contiene datos del usuario.

## 6. Copias de seguridad

La Aplicación tiene activado el mecanismo estándar de copia de seguridad de Android (`android:allowBackup="true"`). Las preferencias de lectura y la posición del último versículo consultado podrían incluirse en la copia de seguridad del dispositivo (Google Drive o cable). Estos datos no son personales ni identificables.

## 7. Conexión a internet

La Aplicación **no se conecta a internet** bajo ninguna circunstancia. No realiza llamadas a servidores, APIs, servicios en la nube, ni descarga contenido de forma remota. Todo el contenido y la funcionalidad de la Aplicación están disponibles sin conexión.

## 8. Servicios de terceros

La Aplicación **no utiliza** servicios de terceros que puedan recopilar datos del usuario. En particular:

- No incluye herramientas de análisis estadístico (Google Analytics, Firebase Analytics, etc.)
- No incluye plataformas publicitarias (AdMob, etc.)
- No incluye servicios de reporte de errores (Firebase Crashlytics, Sentry, etc.)
- No incluye servicios de mensajería push
- No incluye SDK de redes sociales o inicio de sesión de terceros
- No utiliza Google Play Billing ni realiza compras dentro de la aplicación

Las bibliotecas de terceros utilizadas se limitan a frameworks de desarrollo de código abierto (Jetpack Compose, Koin, Kotlinx Coroutines, AndroidX SQLite, AndroidX DataStore), los cuales no recopilan datos del usuario por sí mismos.

## 9. Compartir datos con terceros

La Aplicación **no comparte, vende, cede ni transfiere** ningún dato del usuario a terceros, ya que no recopila datos personales.

## 10. Seguridad

Dado que la Aplicación no recopila ni transmite datos personales, no existen riesgos de filtración de información personal a través de la Aplicación. Los datos almacenados localmente (preferencias de lectura) residen en el espacio privado del sistema operativo Android, accesible únicamente por la propia Aplicación.

## 11. Derechos del usuario

Conforme a las normativas de protección de datos aplicables (incluyendo el Reglamento General de Protección de Datos — RGPD — de la Unión Europea y la Ley de Protección de Datos Personales de los países de habla hispana), el usuario tiene derecho a:

- Acceder a los datos que la Aplicación almacena localmente en su dispositivo
- Eliminar los datos de la Aplicación desde la configuración del dispositivo Android
- Desinstalar la Aplicación en cualquier momento, lo cual elimina todos los datos asociados

Para ejercer estos derechos, el usuario puede contactar al Desarrollador en los medios indicados en la sección de contacto.

## 12. Menores de edad

La Aplicación no está dirigida específicamente a menores de edad. Dado que la Aplicación no recopila ningún dato personal, no existen mecanismos de recopilación de información de menores.

## 13. Cambios a esta Política de Privacidad

El Desarrollador podrá actualizar esta Política de Privacidad en cualquier momento. Cualquier modificación se publicará en esta misma sección con la fecha de última actualización revisada. Se recomienda al usuario revisar esta página periódicamente.

## 14. Contacto

Para cualquier consulta, duda o reclamo relacionado con esta Política de Privacidad, el usuario puede contactar al Desarrollador a través de:

- **Google Play Store:** mediante la sección de comentarios o contacto de la ficha de la aplicación
- **Correo electrónico:** [correo de contacto del desarrollador]

## 15. Vigencia

Esta Política de Privacidad entra en vigor a partir de la fecha de última actualización indicada al inicio del documento y se mantendrá vigente hasta que sea modificada.
