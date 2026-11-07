# JSch loads its ciphers, key exchanges and signatures by class name (JSch.setConfig),
# so R8 cannot see they are used. Keep them all, or SSH fails only in release builds.
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**
# Optional back ends JSch looks for at run time and does without.
-dontwarn org.bouncycastle.**
-dontwarn org.newsclub.**
-dontwarn com.sun.jna.**
-dontwarn org.ietf.jgss.**
-dontwarn org.slf4j.**
-dontwarn org.apache.logging.log4j.**
