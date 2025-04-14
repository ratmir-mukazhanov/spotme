package pt.estga.spotme.ui.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pt.estga.spotme.database.repository.AuthRepository
import pt.estga.spotme.entities.User

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _registrationResult = MutableLiveData<Result<User>>()
    val registrationResult: LiveData<Result<User>> get() = _registrationResult

    fun register(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = authRepository.register(user)
                if (success) {
                    val createdUser = authRepository.getUserByEmail(user.email)
                    if (createdUser != null) {
                        _registrationResult.postValue(Result.success(createdUser))
                    } else {
                        _registrationResult.postValue(Result.failure(Exception("Erro ao recuperar o utilizador.")))
                    }
                } else {
                    _registrationResult.postValue(Result.failure(Exception("Email já registado.")))
                }
            } catch (e: Exception) {
                _registrationResult.postValue(Result.failure(e))
            }
        }
    }
}
