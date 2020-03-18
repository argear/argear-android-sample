package com.seerslab.argear.sample.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CmsService {

    companion object {

        @JvmStatic
        fun createContentsService(baseUrl: String): ContentsApi {

            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC

            val httpClient = OkHttpClient.Builder().run {
                addInterceptor(loggingInterceptor)
                build()
            }

            return Retrofit.Builder().run {
                baseUrl(baseUrl)
                client(httpClient)
                addConverterFactory(GsonConverterFactory.create())
                build()
            }.create(ContentsApi::class.java)
        }
    }
}