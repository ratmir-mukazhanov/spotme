package pt.estga.spotme.ui.parking

import androidx.lifecycle.ViewModel
import pt.estga.spotme.entities.Parking

class ParkingListViewViewModel : ViewModel() {
    val parkings: MutableList<Parking> = mutableListOf()
    var currentOffset: Int = 0
}
