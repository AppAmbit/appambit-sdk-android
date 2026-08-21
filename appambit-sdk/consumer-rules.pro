-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault

-keep @interface com.appambit.sdk.utils.JsonKey
-keep @interface com.appambit.sdk.annotations.DbColumn

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

# Consumer-defined database models can opt into stable column names with @DbColumn.
-keepclassmembers class * {
    @com.appambit.sdk.annotations.DbColumn <fields>;
}
-keepclasseswithmembers,allowoptimization class * {
    @com.appambit.sdk.annotations.DbColumn <fields>;
    public <init>();
}
