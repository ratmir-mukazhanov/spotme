package pt.estga.spotme.ui.parking

import androidx.lifecycle.ViewModel
import pt.estga.spotme.entities.Parking

class ParkingListViewViewModel : ViewModel() {
    var parkings: List<Parking?>? = ArrayList()
    var currentOffset: Int = 0
}