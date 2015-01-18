-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# Uses for ORMLite
-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Uses for EventBus
-keepclassmembers class ** {
    public void onEvent(**);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# ===== Additional =====

# http://stackoverflow.com/questions/21156521/enable-log-with-obfuscation-in-android-using-proguard

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int w(...);
    public static int d(...);
}

-keepattributes Signature
-keepattributes *Annotation*

# http://stackoverflow.com/questions/18481752/gradle-failed-to-build-when-proguard-is-activated
-dontwarn javax.**
-dontwarn org.springframework.**
-dontwarn com.google.common.**

-keep class com.readystatesoftware.sqliteasset.** { *; }

-keep class com.j256.**
-keepclassmembers class com.j256.** { *; }
-keep enum com.j256.**
-keepclassmembers enum com.j256.** { *; }
-keep interface com.j256.**
-keepclassmembers interface com.j256.** { *; }
-keep public class * extends com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper {
    public <init>(android.content.Context);
}


# ----- Involves reflection -----

-keep class com.github.wakhub.monodict.dice.Natives { *; }
-keep class com.github.wakhub.monodict.json.** { *; }
-keepclassmembers class * extends com.github.wakhub.monodict.Model { *; }
