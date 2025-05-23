# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# ZXing barcode library rules - prevent important classes from being removed
-keep class com.google.zxing.** { *; }
-keepclassmembers class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-keepclassmembers class com.journeyapps.barcodescanner.** { *; }

# Keep InputMethodService classes
-keep class android.inputmethodservice.** { *; }
-keep class id.swtkiptr.keyboardscanner.BarcodeInputService { *; }

# Keep custom views
-keep class id.swtkiptr.keyboardscanner.keyboard.** { *; }
-keep class id.swtkiptr.keyboardscanner.scanner.** { *; }

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile