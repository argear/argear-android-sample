package com.seerslab.argear.sample.data

import com.seerslab.argear.sample.AppConfig
import com.seerslab.argear.sample.R
import com.seerslab.argear.session.ARGContents
import com.seerslab.argear.session.ARGContents.BeautyType
import java.util.*

class BeautyItemData {

    private val itemInfo = ArrayList<BeautyItemInfo>()
    private val beautyValues = FloatArray(ARGContents.BEAUTY_TYPE_NUM)

    data class BeautyItemInfo (
        var beautyType: BeautyType,    // default
        var resource1: Int,            // checked
        var resource2: Int
    )

    init {
        initItemInfo()
        initBeautyValue()
    }

    private fun initItemInfo() {
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.VLINE,
                R.drawable.beauty_vline_btn_default,
                R.drawable.beauty_vline_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.FACE_SLIM,
                R.drawable.beauty_face_slim_btn_default,
                R.drawable.beauty_face_slim_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.JAW,
                R.drawable.beauty_jaw_btn_default,
                R.drawable.beauty_jaw_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.CHIN,
                R.drawable.beauty_chin_btn_default,
                R.drawable.beauty_chin_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.EYE,
                R.drawable.beauty_eye_btn_default,
                R.drawable.beauty_eye_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.EYE_GAP,
                R.drawable.beauty_eyegap_btn_default,
                R.drawable.beauty_eyegap_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.NOSE_LINE,
                R.drawable.beauty_nose_line_btn_default,
                R.drawable.beauty_nose_line_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.NOSE_SIDE,
                R.drawable.beauty_nose_side_btn_default,
                R.drawable.beauty_nose_side_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.NOSE_LENGTH,
                R.drawable.beauty_nose_length_btn_default,
                R.drawable.beauty_nose_length_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.MOUTH_SIZE,
                R.drawable.beauty_mouth_size_btn_default,
                R.drawable.beauty_mouth_size_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.EYE_BACK,
                R.drawable.beauty_eyeback_btn_default,
                R.drawable.beauty_eyeback_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.EYE_CORNER,
                R.drawable.beauty_eyecorner_btn_default,
                R.drawable.beauty_eyecorner_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.LIP_SIZE,
                R.drawable.beauty_lip_size_btn_default,
                R.drawable.beauty_lip_size_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.SKIN_FACE,
                R.drawable.beauty_skin_btn_default,
                R.drawable.beauty_skin_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.SKIN_DARK_CIRCLE,
                R.drawable.beauty_dark_circle_btn_default,
                R.drawable.beauty_dark_circle_btn_checked
            )
        )
        itemInfo.add(
            BeautyItemInfo(
                BeautyType.SKIN_MOUTH_WRINKLE,
                R.drawable.beauty_mouth_wrinkle_btn_default,
                R.drawable.beauty_mouth_wrinkle_btn_checked
            )
        )
    }

    fun initBeautyValue() {
        System.arraycopy(
            AppConfig.BEAUTY_TYPE_INIT_VALUE,
            0,
            beautyValues,
            0,
            beautyValues.size
        )
    }

    fun getItemInfoData(): ArrayList<BeautyItemInfo>? {
        return itemInfo
    }

    fun setBeautyValue(beautyType: BeautyType, progress: Float) {
        beautyValues[beautyType.code] = progress
    }

    fun getBeautyValue(beautyType: BeautyType): Float {
        return beautyValues[beautyType.code]
    }

    fun getBeautyValues(): FloatArray? {
        return beautyValues
    }
}