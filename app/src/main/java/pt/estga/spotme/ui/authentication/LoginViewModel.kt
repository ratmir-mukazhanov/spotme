package pt.estga.spotme.ui.authentication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pt.estga.spotme.R
import pt.estga.spotme.database.repository.AuthRepository
import pt.estga.spotme.entities.User

class LoginViewModel(
    application: Application,
    private val authRepository: AuthRepository
) : AndroidViewModel(application) {

    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> get() = _loginResult

    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = authRepository.login(email, password)
                if (user != null) {
                    _loginResult.postValue(Result.success(user))
                } else {
                    val errorMessage = getApplication<Application>().getString(R.string.invalid_credentials)
                    _loginResult.postValue(Result.failure(Exception(errorMessage)))
                }
            } catch (e: Exception) {
                _loginResult.postValue(Result.failure(e))
            }
        }
    }
}