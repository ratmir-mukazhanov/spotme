package pt.estga.spotme.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {
    private val mText = MutableLiveData<String>()

    init {
        mText.value = "This is settings fragment"
    }

    val text: LiveData<String>
        get() = mText
}