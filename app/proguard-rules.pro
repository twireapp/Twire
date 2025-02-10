-dontobfuscate

#  GlideWebpDecoder uses native code to decode webp, keep the jni interface.
-keep public class com.bumptech.glide.integration.webp.WebpImage { *; }
-keep public class com.bumptech.glide.integration.webp.WebpFrame { *; }
-keep public class com.bumptech.glide.integration.webp.WebpBitmapFactory { *; }

-dontwarn me.zhanghai.android.materialprogressbar.HorizontalProgressDrawable
-dontwarn me.zhanghai.android.materialprogressbar.IndeterminateHorizontalProgressDrawable
-dontwarn me.zhanghai.android.materialprogressbar.IndeterminateProgressDrawable

-dontwarn java.beans.BeanInfo
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn java.beans.Transient
-dontwarn java.lang.management.ManagementFactory
-dontwarn javax.management.InstanceAlreadyExistsException
-dontwarn javax.management.InstanceNotFoundException
-dontwarn javax.management.MBeanRegistrationException
-dontwarn javax.management.MBeanServer
-dontwarn javax.management.MalformedObjectNameException
-dontwarn javax.management.NotCompliantMBeanException
-dontwarn javax.management.ObjectInstance
-dontwarn javax.management.ObjectName
-dontwarn javax.management.StandardMBean
