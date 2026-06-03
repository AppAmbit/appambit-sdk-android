package com.appambit.javaapp.models;

import com.appambit.sdk.utils.JsonKey;

import java.util.List;

public class CmsExampleModel {
    public String id;
    public String title;
    public String body;
    public List<String> category;
    public String author;

    @JsonKey("featured_image_url")
    public String featuredImage;

    public int likes;

    public double rating;

    @JsonKey("reading_time")
    public int readingTime;

    @JsonKey("is_published")
    public Boolean isPublished;

    @JsonKey("published_at")
    public String publishedAt;
}
