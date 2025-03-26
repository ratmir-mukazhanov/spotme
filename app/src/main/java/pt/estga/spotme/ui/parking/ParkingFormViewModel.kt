package pt.estga.spotme.ui.parking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pt.estga.spotme.entities.Parking

class ParkingFormViewModel : ViewModel() {
    private val parkingLiveData = MutableLiveData<Parking?>()

    val parking: LiveData<Parking?>
        get() = parkingLiveData

    fun setParking(parking: Parking?) {
        parkingLiveData.value = parking
    }
}