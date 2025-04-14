package pt.estga.spotme.ui.parking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pt.estga.spotme.entities.Parking

class ParkingDetailViewViewModel : ViewModel() {

    private val _parking = MutableLiveData<Parking?>()
    val parking: LiveData<Parking?> = _parking

    fun setParking(parking: Parking) {
        _parking.value = parking
    }

    fun clearParking() {
        _parking.value = null
    }
}