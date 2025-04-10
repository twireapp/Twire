-dontobfuscate

#  GlideWebpDecoder uses native code to decode webp, keep the jni interface.
-keep public class com.bumptech.glide.integration.webp.WebpImage { *; }
-keep public class com.bumptech.glide.integration.webp.WebpFrame { *; }
-keep public class com.bumptech.glide.integration.webp.WebpBitmapFactory { *; }

-dontwarn me.zhanghai.android.materialprogressbar.HorizontalProgressDrawable
-dontwarn me.zhanghai.android.materialprogressbar.IndeterminateHorizontalProgressDrawable
-dontwarn me.zhanghai.android.materialprogressbar.IndeterminateProgressDrawable

-dontwarn java.beans.**
-dontwarn java.lang.management.ManagementFactory
-dontwarn javax.management.**

-dontwarn lombok.Generated
-dontwarn lombok.NonNull

# Twitch4J and Feign
-keep class com.github.twitch4j.common.feign.JsonStringExpander
-keep class com.github.twitch4j.common.feign.ObjectToJsonExpander

# Pagination isn't returned without this
-keep class com.github.twitch4j.helix.domain.HelixPagination

# Apache uses some reflection to find it's logger
-keep class org.apache.commons.logging.impl.LogFactoryImpl
-keep class org.apache.commons.logging.impl.SimpleLog { *; }

# We need to be able to configure Hystrix to use a higher pool limit
-keep class com.netflix.config.** { *; }
