# Room
-keep class androidx.room.RoomDatabase { *; }
-keep class androidx.room.Room { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    <init>();
}

# Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Data Classes (Entities) are already annotated with @Keep, but good to be safe with general rule if needed
-keep class com.h2.wellspend.data.** { *; }

# Aggressively keep all app code to rule out R8 stripping valid code
-keep class com.h2.wellspend.** { *; }

# Keep generated Room implementations
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase_Impl
-dontwarn androidx.room.paging.**

# Keep all interface methods (for DAOs)
-keep interface com.h2.wellspend.data.** { *; }

# Gson specific for TypeToken
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Ensure annotations are kept
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Retrofit 2
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Kotlin Metadata to ensure reflection works (sometimes needed for Gson/Retrofit)
-keep class kotlin.Metadata { *; }

# Explicitly keep network service to be safe
-keep public interface com.h2.wellspend.data.network.GitHubApiService {
    <methods>;
}

# Fix for Retrofit suspend functions (keep Continuation signature)
-keep interface kotlin.coroutines.Continuation { *; }

# Ensure generic types in List<GitHubAsset> are preserved
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.h2.wellspend.data.model.** { *; }

# Aggressively keep Gson to prevent any issues with reflection/types
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken


