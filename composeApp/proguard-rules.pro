# Add project specific ProGuard rules here.
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
