package com.appambit.kotlinapp.models

import com.appambit.sdk.utils.JsonKey

class CmsExampleModel {
    @field:JsonKey("id")
    var id: String? = null
    @field:JsonKey("title")
    var title: String? = null
    @field:JsonKey("body")
    var body: String? = null
    @field:JsonKey("category")
    var category: List<String>? = null
    @field:JsonKey("author")
    var author: String? = null

    @JsonKey("featured_image_url")
    var featuredImage: String? = null

    @field:JsonKey("likes")
    var likes: Int = 0
    @field:JsonKey("rating")
    var rating: Double = 0.0

    @JsonKey("reading_time")
    var readingTime: Int = 0

    @JsonKey("is_published")
    var isPublished: Boolean? = null

    @JsonKey("published_at")
    var publishedAt: String? = null
}
