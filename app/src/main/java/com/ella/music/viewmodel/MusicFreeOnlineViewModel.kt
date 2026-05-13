package com.ella.music.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MusicFreeOnlineViewModel : ViewModel() {
    var importUrl by mutableStateOf("")
    var importExpanded by mutableStateOf(true)
    var isBusy by mutableStateOf(false)
    var message by mutableStateOf("导入 MusicFree 插件后可在这里管理")
}
