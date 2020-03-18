package com.seerslab.argear.sample.model

import com.google.gson.annotations.SerializedName

data class ItemModel(
    @SerializedName("uuid")
    var uuid: String?,

    @SerializedName("title")
    var title: String?,

    @SerializedName("description")
    var description: String?,

    @SerializedName("thumbnail")
    var thumbnailUrl: String?,

    @SerializedName("zip_file")
    var zipFileUrl: String?,

    @SerializedName("num_stickers")
    var numStickers: Int,

    @SerializedName("num_effects")
    var numEffects: Int,

    @SerializedName("num_bgms")
    var numBgms: Int,

    @SerializedName("num_filters")
    var numFilters: Int,

    @SerializedName("num_masks")
    var numMasks: Int,

    @SerializedName("has_trigger")
    var hasTrigger: Boolean,

    @SerializedName("status")
    var status: String?,

    @SerializedName("updated_at")
    var updatedAt: Long,

    @SerializedName("type")
    var type: String?
)