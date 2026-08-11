# kotlinx.serialization keeps its own consumer rules; this covers our @Serializable models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclasseswithmembers class com.momentum.app.data.export.** {
    *** Companion;
}
-keepclasseswithmembers class com.momentum.app.data.export.** {
    kotlinx.serialization.KSerializer serializer(...);
}
