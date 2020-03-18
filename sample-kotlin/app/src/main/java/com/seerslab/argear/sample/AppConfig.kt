package com.seerslab.argear.sample

class AppConfig {

    companion object {

        @JvmField
        val API_URL = "https://apis.argear.io"
        @JvmField
        val API_KEY = "fe49fe8fe8c757e5a4f8d48d"
        @JvmField
        val SECRET_KEY = "5c3b1c9540b4d76134596fd3e9acb2a1aef55245a9980fdd26064ea6b8c5c48c"
        @JvmField
        val AUTH_KEY = "U2FsdGVkX1+Tf2Zzn0Eq/vSbDF2TtyHNnJJEzwqZ0kAzx2OpN7olovPzQRdGUQbeJZO4f8x+LC+8rGkZGt5dAQ=="

        // preference
        @JvmField
        val USER_PREF_NAME = BuildConfig.APPLICATION_ID + ".Preference"
        @JvmField
        val USER_PREF_NAME_FILTER = BuildConfig.APPLICATION_ID + ".ARGearFilter.Preference"
        @JvmField
        val USER_PREF_NAME_STICKER = BuildConfig.APPLICATION_ID + ".ARGearItem.Preference"

        // camera
        // 1: CAMERA_API_1, 2: CAMERA_API_2
        @JvmField
        val USE_CAMERA_API = 1

        // camera ratio
        @JvmField
        val CAMERA_RATIO_FULL = 0
        @JvmField
        val CAMERA_RATIO_4_3 = 1
        @JvmField
        val CAMERA_RATIO_1_1 = 2

        // region - beauty sample
        @JvmField
        val BEAUTY_TYPE_INIT_VALUE = floatArrayOf(10f, 90f, 55f, -50f, 5f, -10f, 0f, 35f, 30f, -35f, 0f, 0f, 0f, 50f, 0f, 0f)

        @JvmField
        val BASIC_BEAUTY_1 = floatArrayOf(20f, 10f, 45f, 45f, 5f, -10f, 40f, 20f, 15f, 0f, 0f, 0f, 0f, 50f, 0f, 0f)

        @JvmField
        val BASIC_BEAUTY_2 = floatArrayOf(10f, 90f, 55f, -50f, 5f, -10f, 0f, 35f, 30f, -35f, 0f, 0f, 0f, 50f, 0f, 0f)

        @JvmField
        val BASIC_BEAUTY_3 = floatArrayOf(25f, 20f, 50f, -25f, 25f, -10f, 30f, 40f, 90f, 0f, 0f, 0f, 0f, 50f, 0f, 0f)
        // endregion
    }
}