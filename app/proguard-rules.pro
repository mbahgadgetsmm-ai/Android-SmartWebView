# ===================================================================
#         PROGUARD RULES - MBAH GADGET (FULL SECURE)
# ===================================================================
# 🔒 Keamanan: Obfuscation + Optimization
# 📦 Size: APK lebih kecil
# ⚡ Performance: Kode lebih efisien
# ===================================================================

# ============================================
# 📌 BASE ANDROID RULES
# ============================================

# Keep default Android classes
-keep class android.** { *; }
-keepclassmembers class android.** { *; }

# Keep support libraries
-keep class androidx.** { *; }
-keepclassmembers class androidx.** { *; }

# Keep Google Play Services
-keep class com.google.android.gms.** { *; }
-keepclassmembers class com.google.android.gms.** { *; }

# Keep Google Material
-keep class com.google.android.material.** { *; }
-keepclassmembers class com.google.android.material.** { *; }

# ============================================
# 🌐 WEBVIEW & JAVASCRIPT INTERFACE
# ============================================

# Keep semua class WebView
-keep class android.webkit.** { *; }
-keepclassmembers class android.webkit.** { *; }

# ⭐ PENTING: Keep semua method dengan @JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep semua class yang dipakai di WebView
-keep class mgks.os.swv.** { *; }
-keepclassmembers class mgks.os.swv.** { *; }

# ============================================
# 🔌 PLUGINS
# ============================================

# Keep semua plugins
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
-keep class com.google.firebase.**$* { *; }
-keepclassmembers class com.google.firebase.**$* { *; }

# ============================================
# 📷 CAMERA & FILE UPLOAD
# ============================================

# Keep FileProvider
-keep class androidx.core.content.FileProvider { *; }
-keepclassmembers class androidx.core.content.FileProvider { *; }

# Keep file processing
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
# 🎯 KEEP ANNOTATIONS & ATTRIBUTES
# ============================================

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# ============================================
# ❌ HAPUS LOG (AMAN UNTUK PRODUCTION)
# ============================================

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
    public static *** getStackTraceString(...);
}

# ============================================
# ⚡ OPTIMIZATION
# ============================================

# Optimize code
-optimizationpasses 5
-dontpreverify
-verbose

# ============================================
# 🚫 JANGAN OBFUSCATE CLASS INI
# ============================================

# Keep MainActivity (entry point)
-keep class mgks.os.swv.MainActivity { *; }
-keepclassmembers class mgks.os.swv.MainActivity { *; }

# Keep Application class
-keep class mgks.os.swv.SWVContext$App { *; }
-keepclassmembers class mgks.os.swv.SWVContext$App { *; }

# Keep Functions
-keep class mgks.os.swv.Functions { *; }
-keepclassmembers class mgks.os.swv.Functions { *; }

# ============================================
# 🛡️ KEEP SEMUA CLASS YANG DIPANGGIL WEBVIEW
# ============================================

# Jika ada class yang dipanggil dari JavaScript, tambahkan di sini
# Contoh:
# -keep class mgks.os.swv.YourJSInterfaceClass { *; }
# -keepclassmembers class mgks.os.swv.YourJSInterfaceClass { *; }

# ============================================
# 📦 MULTIDEX
# ============================================

-keep class androidx.multidex.** { *; }
-keepclassmembers class androidx.multidex.** { *; }

# ============================================
# 🔧 GENERIC RULES
# ============================================

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================
# ✅ FINISH
# ============================================
