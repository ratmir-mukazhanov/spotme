package pt.estga.spotme.ui.parking;

import androidx.lifecycle.ViewModel;
import pt.estga.spotme.entities.Parking;
import java.util.ArrayList;
import java.util.List;

public class ParkingListViewViewModel extends ViewModel {
    private List<Parking> parkings = new ArrayList<>();
    private int currentOffset = 0;

    public List<Parking> getParkings() {
        return parkings;
    }

    public void setParkings(List<Parking> parkings) {
        this.parkings = parkings;
    }

    public int getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(int currentOffset) {
        this.currentOffset = currentOffset;
    }
}