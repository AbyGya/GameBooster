# GameBooster ProGuard Rules
-keepattributes *Annotation*
-keep class com.gamebooster.** { *; }
-keepclassmembers class * extends android.app.Service {
    public void *(android.content.Intent);
}
