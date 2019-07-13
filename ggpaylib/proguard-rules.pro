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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#指定代码的压缩级别
-optimizationpasses 5

 # 混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

#忽略警告
-ignorewarning


##记录生成的日志数据,gradle build时在本项目根目录输出##
#apk 包内所有 class 的内部结构
-dump class_files.txt
#未混淆的类和成员
-printseeds seeds.txt
#列出从 apk 中删除的代码
-printusage unused.txt
#混淆前后的映射
-printmapping mapping.txt
#不混淆资源类
-keepclassmembers class **.R$* {
    public static <fields>;
}
## 定位源类和行数
-keepattributes SourceFile,LineNumberTable

# google 购买混淆
-keep class com.android.vending.billing.**{*;}

-keep class * implements java.io.Serializable {*;}
-keep class com.google.pay.GooglePay{public *;}
-keep class com.google.pay.RandomString{*;}
-keep class com.google.pay.IQueryProductDetailListener{*;}
-keep class com.google.pay.IabHelperCallbackListener{
       *;
}
-keep class com.google.pay.GooglePayStatusListener{*;}

# 删除代码中Log相关的代码
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

