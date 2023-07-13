-dontobfuscate

#  GlideWebpDecoder uses native code to decode webp, keep the jni interface.
-keep public class com.bumptech.glide.integration.webp.WebpImage { *; }
-keep public class com.bumptech.glide.integration.webp.WebpFrame { *; }
-keep public class com.bumptech.glide.integration.webp.WebpBitmapFactory { *; }

-dontwarn me.zhanghai.android.materialprogressbar.HorizontalProgressDrawable
-dontwarn me.zhanghai.android.materialprogressbar.IndeterminateHorizontalProgressDrawable
-dontwarn me.zhanghai.android.materialprogressbar.IndeterminateProgressDrawable
