# ===================================================================
#         PROGUARD RULES - MBAH GADGET (FULL SECURE)
# ===================================================================

# Keep Android core
-keep class android.** { *; }
-keepclassmembers class android.** { *; }

# Keep AndroidX
-keep class androidx.** { *; }
-keepclassmembers class androidx.** { *; }

# Keep Google Play Services
-keep class com.google.android.gms.** { *; }
-keepclassmembers class com.google.android.gms.** { *; }

# Keep Material
-keep class com.google.android.material.** { *; }
-keepclassmembers class com.google.android.material.** { *; }

# ============================================
# 🌐 WEBVIEW & JAVASCRIPT INTERFACE
# ============================================
-keep class android.webkit.** { *; }
-keepclassmembers class android.webkit.** { *; }

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class mgks.os.swv.** { *; }
-keepclassmembers class mgks.os.swv.** { *; }

# ============================================
# 🔌 PLUGINS
# ============================================
-keep class mgks.os.swv.plugins.** { *; }
-keepclassmembers class mgks.os.swv.plugins.** { *; }

# ============================================
# 📱 ONESIGNAL
# ============================================
-keep class com.onesignal.** { *; }
-keepclassmembers class com.onesignal.** { *; }
-keep class com.onesignal.**$* { *; }
-keepclassmembers class com.onesignal.**$* { *; }

# ============================================
# 🔥 FIREBASE
# ============================================
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }

# ============================================
# 📷 CAMERA & FILE
# ============================================
-keep class androidx.core.content.FileProvider { *; }
-keepclassmembers class androidx.core.content.FileProvider { *; }
-keep class mgks.os.swv.FileProcessing { *; }
-keepclassmembers class mgks.os.swv.FileProcessing { *; }

# ============================================
# 🗺️ LOCATION
# ============================================
-keep class com.google.android.gms.location.** { *; }
-keepclassmembers class com.google.android.gms.location.** { *; }

# ============================================
# 🔐 BIOMETRIC
# ============================================
-keep class androidx.biometric.** { *; }
-keepclassmembers class androidx.biometric.** { *; }

# ============================================
# 📦 ZXING (QR SCANNER)
# ============================================
-keep class com.journeyapps.barcodescanner.** { *; }
-keepclassmembers class com.journeyapps.barcodescanner.** { *; }

# ============================================
# 🎯 KEEP ANNOTATIONS
# ============================================
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# ============================================
# ❌ HAPUS LOG (AMAN)
# ============================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# ============================================
# ⚡ OPTIMIZATION
# ============================================
-optimizationpasses 5
-dontpreverify
-verbose

# ============================================
# 🚫 JANGAN OBFUSCATE
# ============================================
-keep class mgks.os.swv.MainActivity { *; }
-keepclassmembers class mgks.os.swv.MainActivity { *; }
-keep class mgks.os.swv.SWVContext$App { *; }
-keepclassmembers class mgks.os.swv.SWVContext$App { *; }
-keep class mgks.os.swv.Functions { *; }
-keepclassmembers class mgks.os.swv.Functions { *; }

# ============================================
# 📦 MULTIDEX
# ============================================
-keep class androidx.multidex.** { *; }
-keepclassmembers class androidx.multidex.** { *; }

# ============================================
# 🔧 GENERIC
# ============================================
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
