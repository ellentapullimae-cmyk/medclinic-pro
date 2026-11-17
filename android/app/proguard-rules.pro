-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.clinica.** {
    *** Companion;
}
-keepclasseswithmembers class com.clinica.** {
    kotlinx.serialization.KSerializer serializer(...);
}
