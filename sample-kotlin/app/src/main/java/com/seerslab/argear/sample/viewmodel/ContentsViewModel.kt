package com.seerslab.argear.sample.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.seerslab.argear.sample.AppConfig
import com.seerslab.argear.sample.api.ContentsResponse

class ContentsViewModel(application: Application) : AndroidViewModel(application) {

    private val mutableLiveData: MutableLiveData<ContentsResponse>
    private val contentsRepository: ContentsRepository = ContentsRepository.instance

    val contents: LiveData<ContentsResponse>
        get() = mutableLiveData

    init {
        mutableLiveData = contentsRepository.getContents(AppConfig.API_KEY)
    }
}