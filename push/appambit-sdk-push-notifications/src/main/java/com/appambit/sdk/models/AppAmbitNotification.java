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
    private final String ticker;
    private final Boolean sticky;
    private final String visibility;
    private final String channelId;
    private final String priority;
    private final String tag;
    private final String sound;
    private final String clickAction;

    public AppAmbitNotification(
            @Nullable String title,
            @Nullable String body,
            @Nullable String color,
            @Nullable String smallIconName,
            @Nullable String imageUrl,
            @NonNull Map<String, String> data
    ) {
        this(title, body, color, smallIconName, imageUrl, data,
                null, null, null, null, null, null, null, null);
    }
    public AppAmbitNotification(
            @Nullable String title,
            @Nullable String body,
            @Nullable String color,
            @Nullable String smallIconName,
            @Nullable String imageUrl,
            @NonNull Map<String, String> data,
            @Nullable String ticker,
            @Nullable Boolean sticky,
            @Nullable String visibility,
            @Nullable String channelId,
            @Nullable String priority,
            @Nullable String tag,
            @Nullable String sound,
            @Nullable String clickAction
    ) {
        this.title = title;
        this.body = body;
        this.color = color;
        this.smallIconName = smallIconName;
        this.imageUrl = imageUrl;
        this.data = data;
        this.ticker = ticker;
        this.sticky = sticky;
        this.visibility = visibility;
        this.channelId = channelId;
        this.priority = priority;
        this.tag = tag;
        this.sound = sound;
        this.clickAction = clickAction;
    }
    @Nullable public String getTitle()          { return title; }
    @Nullable public String getBody()           { return body; }
    @Nullable public String getColor()          { return color; }
    @Nullable public String getSmallIconName()  { return smallIconName; }
    @Nullable public String getImageUrl()       { return imageUrl; }
    @NonNull  public Map<String, String> getData() { return data; }
    @Nullable public String  getTicker()      { return ticker; }
    @Nullable public Boolean getSticky()      { return sticky; }
    @Nullable public String  getVisibility()  { return visibility; }
    @Nullable public String  getChannelId()   { return channelId; }
    @Nullable public String  getPriority()    { return priority; }
    @Nullable public String  getTag()         { return tag; }
    @Nullable public String  getSound()       { return sound; }
    @Nullable public String  getClickAction() { return clickAction; }
}