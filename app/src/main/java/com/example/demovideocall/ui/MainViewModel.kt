package com.example.demovideocall.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demovideocall.ui.service.LoginManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by ThanhTran on 7/6/2022.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val loginManager: LoginManager
) : ViewModel() {

    fun getToken() {
        viewModelScope.launch {
            loginManager.signIn("nghia6", "nghia6")
        }
    }

}