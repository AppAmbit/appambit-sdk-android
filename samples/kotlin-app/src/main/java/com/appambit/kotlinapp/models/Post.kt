package com.appambit.kotlinapp.models

import com.appambit.sdk.utils.JsonKey

class Post {
    @JvmField var id: String? = null
    @JvmField var title: String? = null
    @JvmField var body: String? = null
    @JvmField var category: String? = null
    @JvmField var author: String? = null

    @JvmField
    @JsonKey("featured_image")
    var featuredImage: String? = null

    @JvmField var likes: Int = 0
    @JvmField var rating: Double = 0.0

    @JvmField
    @JsonKey("reading_time")
    var readingTime: Int = 0

    @JvmField
    @JsonKey("published_at")
    var publishedAt: String? = null
}
