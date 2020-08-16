package com.example.audiobook.model
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize


data class Book(
    @SerializedName("ListenHubAudioBooks")
    val listenHubAudioBooks: List<ListenHubAudioBooks>
)
@Parcelize
data class ListenHubAudioBooks(
    @SerializedName("authors")
    val authors: List<Author>,
    @SerializedName("chapterCount")
    val chapterCount: String,
    @SerializedName("chapters")
    val chapters: List<Chapter>,
    @SerializedName("description")
    val description: String,
    @SerializedName("genres")
    val genres: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("imageUrl")
    val imageUrl: String,
    @SerializedName("language")
    val language: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("totaltime")
    val totaltime: String,
    @SerializedName("totaltimesecs")
    val totaltimesecs: Int,
    @SerializedName("url_zip_file")
    val urlZipFile: String
): Parcelable

@Parcelize
data class Author(
    @SerializedName("dob")
    val dob: String,
    @SerializedName("dod")
    val dod: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("last_name")
    val lastName: String
): Parcelable

@Parcelize
data class Chapter(
    @SerializedName("duration")
    val duration: String,
    @SerializedName("link")
    val link: String,
    @SerializedName("narratedBy")
    val narratedBy: String,
    @SerializedName("title")
    val title: String
): Parcelable

@Parcelize
data class SeekBarUpdates(
    val position: Int,
    val duration: Long?
) : Parcelable