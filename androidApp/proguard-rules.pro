# Rutas de navegación type-safe.
#
# `navigation-compose` serializa y deserializa estas clases para construir los
# argumentos de cada destino, y lo hace buscando su serializador generado. Si
# R8 les cambia el nombre o se lleva por delante el `Companion`, la app
# compila pero revienta al navegar.
-keep @kotlinx.serialization.Serializable class com.comentariobiblico.app.ui.nav.** { *; }
-keepclassmembers class com.comentariobiblico.app.ui.nav.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Modelos de dominio: `lemma_spans` se deserializa con kotlinx-serialization.
-keepclassmembers class com.comentariobiblico.app.domain.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# El resto (androidx.sqlite con su JNI, Koin, Compose, DataStore) trae sus
# propias reglas de consumidor en los AAR; no hace falta repetirlas aquí.
