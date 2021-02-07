package com.suihan74.hatena.bookmark

import com.suihan74.hatena.bookmark.serializer.InstantISO8601Serializer
import com.suihan74.hatena.star.StarCount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/** ipad.entry_bookmarks.jsonのレスポンス */
@Serializable
data class Bookmark(
    @SerialName("user")
    private val mUser : User,

    val comment : String,

    @SerialName("is_private")
    val isPrivate : Boolean,

    val link : String,

    val tags : List<String>,

    @Serializable(with = InstantISO8601Serializer::class)
    val timestamp : Instant,

    @SerialName("star_count")
    val starCount : List<StarCount>
) {
    val user: String by lazy { mUser.name }

    val userIconUrl: String by lazy { mUser.profileImageUrl }

    // ------ //

    @Serializable
    data class User(
        val name : String,

        @SerialName("profile_image_url")
        val profileImageUrl : String
    )
}