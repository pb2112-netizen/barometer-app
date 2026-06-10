# kotlinx.serialization — zachowaj metadane serializatorów dla modeli @Serializable
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.worldbarometer.app.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.worldbarometer.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
