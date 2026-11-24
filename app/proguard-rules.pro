# libxposed API 101 模块入口（R8 会重写 java_init.list）
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# JNI 桥：类名、方法名与 C++ 符号、GetStaticMethodID 查找保持一致
-keep class io.github.hexstr.UnityFPSUnlocker.nativebridge.NativeBridge { *; }

# androidx.window 为部分 androidx 库的可选依赖
-dontwarn androidx.window.**
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.core.**
