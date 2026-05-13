package com.appambit.sdk.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Map;

public class AppAmbitNotification {

    private final String title;
    private final String body;
    private final String color;
    private final String smallIconName;
    private final String imageUrl;
    private final Map<String, String> data;

    public AppAmbitNotification(
            @Nullable String title,
            @Nullable String body,
            @Nullable String color,
            @Nullable String smallIconName,
            @Nullable String imageUrl,
            @NonNull Map<String, String> data
    ) {
        this.title = title;
        this.body = body;
        this.color = color;
        this.smallIconName = smallIconName;
        this.imageUrl = imageUrl;
        this.data = data;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getBody() {
        return body;
    }

    @Nullable
    public String getColor() {
        return color;
    }

    @Nullable
    public String getSmallIconName() {
        return smallIconName;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    @NonNull
    public Map<String, String> getData() {
        return data;
    }
}
