package pt.estga.spotme.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import pt.estga.spotme.utils.UserSession

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val _userid = MutableLiveData<Long?>()
    val userId: LiveData<Long?> = _userid

    private val _userName = MutableLiveData<String?>()
    val userName: MutableLiveData<String?> = _userName

    private val _userEmail = MutableLiveData<String?>()
    val userEmail: MutableLiveData<String?> = _userEmail

    private val _userImagePath = MutableLiveData<String?>()
    val userImagePath: LiveData<String?> = _userImagePath

    fun loadUserData() {
        val session = UserSession.getInstance(getApplication())
        _userid.value = session.userId
        _userName.value = session.userName
        _userEmail.value = session.userEmail
        _userImagePath.value = session.userProfileImage
    }

    fun updateUser(name: String?, email: String?, imagePath: String?) {
        _userName.value = name
        _userEmail.value = email
        _userImagePath.value = imagePath
    }
}
