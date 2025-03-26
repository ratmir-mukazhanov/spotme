package pt.estga.spotme.ui.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AccountViewModel : ViewModel() {
    private val mText = MutableLiveData<String>()

    init {
        mText.value = "This is gallery fragment"
    }

    val text: LiveData<String>
        get() = mText
}