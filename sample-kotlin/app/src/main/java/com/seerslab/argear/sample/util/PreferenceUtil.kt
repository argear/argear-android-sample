package com.seerslab.argear.sample.util

import android.content.Context

class PreferenceUtil {

    companion object {
        fun putLongValue(context: Context, prefName: String, key: String, value: Long) {
            val pref = context.getSharedPreferences(prefName, 0)
            val editor = pref.edit()
            editor.putLong(key, value)
            editor.apply()
        }

        fun getLongValue(context: Context, prefName: String, key: String): Long {
            val pref = context.getSharedPreferences(prefName, 0)
            return try {
                pref.getLong(key, 0)
            } catch (e: Exception) {
                0
            }
        }

        fun putStringValue(context: Context, prefName: String, key: String, value: String) {
            val pref = context.getSharedPreferences(prefName, 0)
            val editor = pref.edit()
            editor.putString(key, value)
            editor.apply()
        }

        fun getStringValue(context: Context, prefName: String, key: String): String? {
            return try {
                val pref = context.getSharedPreferences(prefName, 0)
                pref.getString(key, null)
            } catch (e: Exception) {
                null
            }
        }
    }
}