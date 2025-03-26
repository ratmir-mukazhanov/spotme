package pt.estga.spotme.ui.parking;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import pt.estga.spotme.entities.Parking;

public class ParkingFormViewModel extends ViewModel {
    private final MutableLiveData<Parking> parkingLiveData = new MutableLiveData<>();

    public LiveData<Parking> getParking() {
        return parkingLiveData;
    }

    public void setParking(Parking parking) {
        parkingLiveData.setValue(parking);
    }

}