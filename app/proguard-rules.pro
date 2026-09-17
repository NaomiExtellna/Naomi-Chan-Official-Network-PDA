# Project-specific R8/ProGuard rules.
#
# The release build is minified for the low-RAM SUNMI V2. Most AndroidX, Room,
# CameraX and ML Kit dependencies ship their own consumer rules, so keep this
# file deliberately narrow and preserve only the vendor printer IPC surface.

# SUNMI printer SDK classes cross a vendor service/AIDL boundary. Keep the
# public SDK surface intact so release shrinking cannot rename or strip classes
# the SUNMI OS printer service expects to resolve.
-keep class com.sunmi.peripheral.printer.** { *; }
-keep interface com.sunmi.peripheral.printer.** { *; }
-dontwarn com.sunmi.peripheral.printer.**

# Preserve runtime annotations/signatures used by Android frameworks and
# libraries that perform reflection while still allowing ordinary code shrink.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault,Signature
