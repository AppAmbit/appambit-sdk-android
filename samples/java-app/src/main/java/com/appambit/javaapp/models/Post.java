package com.appambit.javaapp.models;

import com.appambit.sdk.utils.JsonKey;

public class Post {
    public String id;
    public String title;
    public String body;
    public String category;
    public String author;

    @JsonKey("featured_image")
    public String featuredImage;

    public int likes;

    public double rating;

    @JsonKey("reading_time")
    public int readingTime;

    @JsonKey("published_at")
    public String publishedAt;
}
