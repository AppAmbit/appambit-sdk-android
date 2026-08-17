-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault

-keep @interface com.appambit.sdk.utils.JsonKey

# SDK response models are mapped by field name and may include inherited fields.
-keepclassmembers class com.appambit.sdk.models.** {
    <fields>;
}
-keepclassmembers class com.appambit.sdk.models.** {
    public <init>();
}

# Consumer-defined models can opt into stable JSON names with @JsonKey.
-keepclassmembers class * {
    @com.appambit.sdk.utils.JsonKey <fields>;
}
-keepclasseswithmembers,allowoptimization class * {
    @com.appambit.sdk.utils.JsonKey <fields>;
    public <init>();
}
