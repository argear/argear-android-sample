package com.seerslab.argear.sample.model

import com.google.gson.annotations.SerializedName

data class CategoryModel (
    @SerializedName("uuid")
    var uuid: String?,

    @SerializedName("title")
    var title: String?,

    @SerializedName("description")
    var description: String?,

    @SerializedName("is_bundle")
    var isBundle: Boolean,

    @SerializedName("updated_at")
    var updatedAt: Long,

    @SerializedName("status")
    var status: String?,

    @SerializedName("items")
    var items: List<ItemModel>?
)