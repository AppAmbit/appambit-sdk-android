package com.appambit.javaapp.models;

import com.appambit.sdk.utils.JsonKey;

import java.util.List;

public class CmsExampleModel {
    @JsonKey("id")
    public String id;
    @JsonKey("title")
    public String title;
    @JsonKey("body")
    public String body;
    @JsonKey("category")
    public List<String> category;
    @JsonKey("author")
    public String author;

    @JsonKey("featured_image_url")
    public String featuredImage;

    @JsonKey("likes")
    public int likes;

    @JsonKey("rating")
    public double rating;

    @JsonKey("reading_time")
    public int readingTime;

    @JsonKey("is_published")
    public Boolean isPublished;

    @JsonKey("published_at")
    public String publishedAt;
}
