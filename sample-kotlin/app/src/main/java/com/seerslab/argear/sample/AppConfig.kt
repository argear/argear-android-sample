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
        val AUTH_KEY = "U2FsdGVkX1+k8JjhmPLPROT1F7wdSAiW08DhJymQnqpYdU/VaaSR5BwYgLzfiRsS2LXFf1YXCjfryqzIo4/M0g=="

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

        // region - beauty sample
        @JvmField
        val BEAUTY_TYPE_INIT_VALUE = floatArrayOf(
            10f,     //VLINE
            90f,     //ACE_SLIM
            55f,     //JAW
            -50f,    //CHIN
            5f,      //EYE
            -10f,    //EYE_GAP
            0f,      //NOSE_LINE
            35f,     //NOSE_SIDE
            30f,     //NOSE_LENGTH
            -35f,    //MOUTH_SIZE
            0f,      //EYE_BACK
            0f,      //EYE_CORNER
            0f,      //LIP_SIZE
            50f,     //SKIN
            0f,      //DARK_CIRCLE
            0f       //MOUTH_WRINKLE
        )

        @JvmField
        val BASIC_BEAUTY_1 = floatArrayOf(20f, 10f, 45f, 45f, 5f, -10f, 40f, 20f, 15f, 0f, 0f, 0f, 0f, 50f, 0f, 0f)

        @JvmField
        val BASIC_BEAUTY_2 = floatArrayOf(10f, 90f, 55f, -50f, 5f, -10f, 0f, 35f, 30f, -35f, 0f, 0f, 0f, 50f, 0f, 0f)

        @JvmField
        val BASIC_BEAUTY_3 = floatArrayOf(25f, 20f, 50f, -25f, 25f, -10f, 30f, 40f, 90f, 0f, 0f, 0f, 0f, 50f, 0f, 0f)
        // endregion
    }
}