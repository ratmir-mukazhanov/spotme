package pt.estga.spotme.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private val mText = MutableLiveData<String>()

    init {
        mText.value = "This is home fragment"
    }

    val text: LiveData<String>
        get() = mText
}