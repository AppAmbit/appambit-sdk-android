package com.appambit.kotlinapp.models

import com.appambit.sdk.utils.JsonKey

class CmsExampleModel {
    var id: String? = null
    var title: String? = null
    var body: String? = null
    var category: List<String>? = null
    var author: String? = null

    @JsonKey("featured_image")
    var featuredImage: String? = null

    var likes: Int = 0
    var rating: Double = 0.0

    @JsonKey("reading_time")
    var readingTime: Int = 0

    @JsonKey("is_published")
    var isPublished: Boolean? = null

    @JsonKey("published_at")
    var publishedAt: String? = null
}
