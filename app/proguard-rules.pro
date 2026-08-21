# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes InnerClasses
-dontwarn kotlinx.serialization.**
-keep,includedescriptorclasses class com.metehanyl.borsa.**$$serializer { *; }
-keepclassmembers class com.metehanyl.borsa.** {
    *** Companion;
}
-keepclasseswithmembers class com.metehanyl.borsa.** {
    kotlinx.serialization.KSerializer serializer(...);
}
